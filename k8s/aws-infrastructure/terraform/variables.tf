variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod."
  }
}

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "domain_name" {
  description = "Domain name for the application (leave empty if not using custom domain)"
  type        = string
  default     = ""
}

variable "enable_nat_gateway" {
  description = "Should be true to provision NAT Gateways for each of your private networks"
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Should be true to provision a single shared NAT Gateway across all of your private networks"
  type        = bool
  default     = false
}

variable "enable_dns_hostnames" {
  description = "Should be true to enable DNS hostnames in the VPC"
  type        = bool
  default     = true
}

variable "enable_dns_support" {
  description = "Should be true to enable DNS support in the VPC"
  type        = bool
  default     = true
}

# EKS Variables
variable "cluster_version" {
  description = "Kubernetes version to use for the EKS cluster"
  type        = string
  default     = "1.28"
}

variable "node_groups" {
  description = "EKS node groups configuration"
  type = map(object({
    instance_types = list(string)
    capacity_type  = string
    min_size      = number
    max_size      = number
    desired_size  = number
    ami_type      = string
    labels        = map(string)
    taints        = map(object({
      key    = string
      value  = string
      effect = string
    }))
  }))
  default = {
    application = {
      instance_types = ["t3.medium", "t3.large"]
      capacity_type  = "ON_DEMAND"
      min_size      = 3
      max_size      = 15
      desired_size  = 3
      ami_type      = "AL2_x86_64"
      labels = {
        NodeType = "application"
      }
      taints = {
        application = {
          key    = "application-nodes"
          value  = "true"
          effect = "NO_SCHEDULE"
        }
      }
    }
    system = {
      instance_types = ["t3.small", "t3.medium"]
      capacity_type  = "ON_DEMAND"
      min_size      = 2
      max_size      = 4
      desired_size  = 2
      ami_type      = "AL2_x86_64"
      labels = {
        NodeType = "system"
      }
      taints = {}
    }
  }
}

# RDS Variables
variable "rds_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.r6g.large"
}

variable "rds_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
  default     = 100
}

variable "rds_max_allocated_storage" {
  description = "RDS maximum allocated storage in GB"
  type        = number
  default     = 1000
}

variable "rds_backup_retention_period" {
  description = "RDS backup retention period in days"
  type        = number
  default     = 7
}

variable "rds_backup_window" {
  description = "RDS backup window"
  type        = string
  default     = "03:00-06:00"
}

variable "rds_maintenance_window" {
  description = "RDS maintenance window"
  type        = string
  default     = "Mon:00:00-Mon:03:00"
}

variable "rds_skip_final_snapshot" {
  description = "Skip final snapshot when deleting RDS instance"
  type        = bool
  default     = true
}

variable "rds_deletion_protection" {
  description = "Enable deletion protection for RDS instance"
  type        = bool
  default     = false
}

# ElastiCache Variables
variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "cache.r6g.medium"
}

variable "redis_num_cache_clusters" {
  description = "Number of cache clusters for Redis"
  type        = number
  default     = 2
}

variable "redis_parameter_group_name" {
  description = "Parameter group name for Redis"
  type        = string
  default     = "default.redis7"
}

variable "redis_port" {
  description = "Port number on which Redis accepts connections"
  type        = number
  default     = 6379
}

variable "redis_maintenance_window" {
  description = "Maintenance window for Redis"
  type        = string
  default     = "sun:05:00-sun:09:00"
}

variable "redis_snapshot_retention_limit" {
  description = "Number of days for which ElastiCache retains automatic cache cluster snapshots"
  type        = number
  default     = 1
}

variable "redis_snapshot_window" {
  description = "Daily time range for ElastiCache snapshots"
  type        = string
  default     = "03:00-05:00"
}

# MSK Variables
variable "kafka_version" {
  description = "Kafka version"
  type        = string
  default     = "3.5.1"
}

variable "kafka_number_of_broker_nodes" {
  description = "Number of broker nodes in Kafka cluster"
  type        = number
  default     = 3
}

variable "kafka_instance_type" {
  description = "Kafka broker instance type"
  type        = string
  default     = "kafka.m5.large"
}

variable "kafka_ebs_volume_size" {
  description = "EBS volume size for Kafka brokers in GB"
  type        = number
  default     = 100
}

variable "kafka_log_retention_hours" {
  description = "Kafka log retention in hours"
  type        = number
  default     = 168
}

variable "kafka_log_retention_bytes" {
  description = "Kafka log retention in bytes"
  type        = string
  default     = "1073741824"
}

# S3 Variables
variable "s3_versioning_enabled" {
  description = "Enable versioning for S3 buckets"
  type        = bool
  default     = true
}

variable "s3_force_destroy" {
  description = "Force destroy S3 buckets (use with caution)"
  type        = bool
  default     = false
}

# Load Balancer Variables
variable "enable_deletion_protection" {
  description = "Enable deletion protection for load balancers"
  type        = bool
  default     = false
}

# Monitoring Variables
variable "cloudwatch_log_retention" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 14
}

# Tags
variable "additional_tags" {
  description = "Additional tags to apply to all resources"
  type        = map(string)
  default     = {}
}