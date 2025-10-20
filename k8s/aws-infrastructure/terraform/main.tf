# Terraform configuration for Zendapag AWS Infrastructure

terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }

  backend "s3" {
    bucket         = "zendapag-terraform-state"
    key            = "infrastructure/terraform.tfstate"
    region         = "us-west-2"
    encrypt        = true
    dynamodb_table = "zendapag-terraform-locks"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

data "aws_eks_cluster" "cluster" {
  name = module.eks.cluster_id
}

data "aws_eks_cluster_auth" "cluster" {
  name = module.eks.cluster_id
}

provider "kubernetes" {
  host                   = data.aws_eks_cluster.cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.cluster.token
}

provider "helm" {
  kubernetes {
    host                   = data.aws_eks_cluster.cluster.endpoint
    cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority[0].data)
    token                  = data.aws_eks_cluster_auth.cluster.token
  }
}

# Local values
locals {
  name_prefix = "zendapag-${var.environment}"

  common_tags = {
    Project     = "zendapag"
    Environment = var.environment
    ManagedBy   = "terraform"
    Owner       = "zendapag-team"
  }

  azs = slice(data.aws_availability_zones.available.names, 0, 3)
}

# Data sources
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

# VPC
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${local.name_prefix}-vpc"
  cidr = var.vpc_cidr

  azs             = local.azs
  private_subnets = [for k, v in local.azs : cidrsubnet(var.vpc_cidr, 4, k)]
  public_subnets  = [for k, v in local.azs : cidrsubnet(var.vpc_cidr, 8, k + 48)]
  intra_subnets   = [for k, v in local.azs : cidrsubnet(var.vpc_cidr, 8, k + 52)]

  enable_nat_gateway   = true
  single_nat_gateway   = var.environment != "prod"
  enable_vpn_gateway   = false
  enable_dns_hostnames = true
  enable_dns_support   = true

  # VPC Flow Logs
  enable_flow_log                      = true
  create_flow_log_cloudwatch_iam_role  = true
  create_flow_log_cloudwatch_log_group = true

  public_subnet_tags = {
    "kubernetes.io/role/elb" = 1
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = 1
  }

  tags = local.common_tags
}

# EKS Cluster
module "eks" {
  source = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = "${local.name_prefix}-cluster"
  cluster_version = "1.28"

  vpc_id                         = module.vpc.vpc_id
  subnet_ids                     = module.vpc.private_subnets
  cluster_endpoint_public_access = true
  cluster_endpoint_private_access = true

  cluster_addons = {
    coredns = {
      most_recent = true
    }
    kube-proxy = {
      most_recent = true
    }
    vpc-cni = {
      most_recent = true
    }
    aws-ebs-csi-driver = {
      most_recent = true
    }
  }

  eks_managed_node_groups = {
    application = {
      name = "application"

      instance_types = ["t3.medium", "t3.large"]
      capacity_type  = "ON_DEMAND"

      min_size     = 3
      max_size     = 15
      desired_size = var.environment == "prod" ? 6 : 3

      ami_type = "AL2_x86_64"

      labels = {
        Environment = var.environment
        NodeType    = "application"
      }

      taints = {
        application = {
          key    = "application-nodes"
          value  = "true"
          effect = "NO_SCHEDULE"
        }
      }

      tags = merge(local.common_tags, {
        Name = "${local.name_prefix}-application-nodes"
      })
    }

    system = {
      name = "system"

      instance_types = ["t3.small", "t3.medium"]
      capacity_type  = "ON_DEMAND"

      min_size     = 2
      max_size     = 4
      desired_size = 2

      ami_type = "AL2_x86_64"

      labels = {
        Environment = var.environment
        NodeType    = "system"
      }

      tags = merge(local.common_tags, {
        Name = "${local.name_prefix}-system-nodes"
      })
    }
  }

  # Cluster access entry
  access_entries = {
    zendapag_admin = {
      kubernetes_groups = []
      principal_arn     = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/ZendapagAdminRole"

      policy_associations = {
        admin = {
          policy_arn = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
          access_scope = {
            type = "cluster"
          }
        }
      }
    }
  }

  tags = local.common_tags
}

# RDS PostgreSQL
module "rds" {
  source = "terraform-aws-modules/rds/aws"
  version = "~> 6.0"

  identifier = "${local.name_prefix}-db"

  engine               = "postgres"
  engine_version       = "15.4"
  family               = "postgres15"
  major_engine_version = "15"
  instance_class       = var.environment == "prod" ? "db.r6g.xlarge" : "db.r6g.large"

  allocated_storage     = var.environment == "prod" ? 200 : 100
  max_allocated_storage = var.environment == "prod" ? 1000 : 500
  storage_encrypted     = true

  db_name  = "zendapag"
  username = "zendapag"
  port     = 5432

  multi_az               = var.environment == "prod"
  publicly_accessible    = false
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = module.vpc.database_subnet_group

  maintenance_window              = "Mon:00:00-Mon:03:00"
  backup_window                  = "03:00-06:00"
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  create_cloudwatch_log_group     = true

  backup_retention_period = var.environment == "prod" ? 30 : 7
  skip_final_snapshot     = var.environment != "prod"
  deletion_protection     = var.environment == "prod"

  performance_insights_enabled          = true
  performance_insights_retention_period = var.environment == "prod" ? 7 : 7
  create_monitoring_role                = true
  monitoring_interval                   = 60

  parameters = [
    {
      name  = "log_checkpoints"
      value = 1
    },
    {
      name  = "log_connections"
      value = 1
    },
    {
      name  = "log_disconnections"
      value = 1
    },
    {
      name  = "log_lock_waits"
      value = 1
    },
    {
      name  = "log_min_duration_statement"
      value = 100
    },
    {
      name  = "shared_preload_libraries"
      value = "pg_stat_statements"
    }
  ]

  tags = local.common_tags
}

# ElastiCache Redis
resource "aws_elasticache_subnet_group" "redis" {
  name       = "${local.name_prefix}-cache-subnet"
  subnet_ids = module.vpc.intra_subnets

  tags = local.common_tags
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id         = "${local.name_prefix}-redis"
  description                  = "Redis cluster for Zendapag ${var.environment}"

  node_type            = var.environment == "prod" ? "cache.r6g.large" : "cache.r6g.medium"
  port                 = 6379
  parameter_group_name = "default.redis7"

  num_cache_clusters         = var.environment == "prod" ? 3 : 2
  automatic_failover_enabled = var.environment == "prod"
  multi_az_enabled          = var.environment == "prod"

  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = random_password.redis_auth.result

  maintenance_window         = "sun:05:00-sun:09:00"
  snapshot_retention_limit   = var.environment == "prod" ? 5 : 1
  snapshot_window           = "03:00-05:00"
  final_snapshot_identifier = "${local.name_prefix}-redis-final-snapshot"

  log_delivery_configuration {
    destination      = aws_cloudwatch_log_group.redis.name
    destination_type = "cloudwatch-logs"
    log_format       = "text"
    log_type         = "slow-log"
  }

  tags = local.common_tags
}

resource "random_password" "redis_auth" {
  length  = 32
  special = true
}

resource "aws_cloudwatch_log_group" "redis" {
  name              = "/aws/elasticache/${local.name_prefix}"
  retention_in_days = var.environment == "prod" ? 14 : 7

  tags = local.common_tags
}

# MSK Kafka Cluster
resource "aws_msk_cluster" "kafka" {
  cluster_name           = "${local.name_prefix}-kafka"
  kafka_version          = "3.5.1"
  number_of_broker_nodes = var.environment == "prod" ? 6 : 3

  broker_node_group_info {
    instance_type = var.environment == "prod" ? "kafka.m5.xlarge" : "kafka.m5.large"
    storage_info {
      ebs_storage_info {
        volume_size = var.environment == "prod" ? 500 : 100
      }
    }
    client_subnets  = module.vpc.private_subnets
    security_groups = [aws_security_group.msk.id]
  }

  client_authentication {
    tls {}
  }

  configuration_info {
    arn      = aws_msk_configuration.kafka.arn
    revision = aws_msk_configuration.kafka.latest_revision
  }

  encryption_info {
    encryption_at_rest_kms_key_id = aws_kms_key.msk.arn
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.msk.name
      }
      s3 {
        enabled = var.environment == "prod"
        bucket  = var.environment == "prod" ? aws_s3_bucket.logs[0].id : null
        prefix  = "msk-logs/"
      }
    }
  }

  tags = local.common_tags
}

resource "aws_msk_configuration" "kafka" {
  kafka_versions = ["3.5.1"]
  name           = "${local.name_prefix}-config"

  server_properties = <<PROPERTIES
auto.create.topics.enable=false
default.replication.factor=3
min.insync.replicas=2
num.partitions=3
log.retention.hours=168
log.retention.bytes=1073741824
log.segment.bytes=1073741824
PROPERTIES
}

resource "aws_kms_key" "msk" {
  description = "KMS key for MSK encryption"
  tags = local.common_tags
}

resource "aws_kms_alias" "msk" {
  name          = "alias/${local.name_prefix}-msk"
  target_key_id = aws_kms_key.msk.key_id
}

resource "aws_cloudwatch_log_group" "msk" {
  name              = "/aws/msk/${local.name_prefix}"
  retention_in_days = var.environment == "prod" ? 14 : 7

  tags = local.common_tags
}