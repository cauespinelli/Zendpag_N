# VPC Outputs
output "vpc_id" {
  description = "ID of the VPC"
  value       = module.vpc.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = module.vpc.vpc_cidr_block
}

output "private_subnets" {
  description = "List of IDs of private subnets"
  value       = module.vpc.private_subnets
}

output "public_subnets" {
  description = "List of IDs of public subnets"
  value       = module.vpc.public_subnets
}

output "intra_subnets" {
  description = "List of IDs of intra subnets"
  value       = module.vpc.intra_subnets
}

output "database_subnets" {
  description = "List of IDs of database subnets"
  value       = module.vpc.database_subnets
}

output "availability_zones" {
  description = "List of availability zones"
  value       = local.azs
}

# EKS Outputs
output "cluster_id" {
  description = "EKS cluster ID"
  value       = module.eks.cluster_id
}

output "cluster_arn" {
  description = "EKS cluster ARN"
  value       = module.eks.cluster_arn
}

output "cluster_endpoint" {
  description = "Endpoint for EKS control plane"
  value       = module.eks.cluster_endpoint
}

output "cluster_version" {
  description = "EKS cluster version"
  value       = module.eks.cluster_version
}

output "cluster_platform_version" {
  description = "Platform version for the EKS cluster"
  value       = module.eks.cluster_platform_version
}

output "cluster_security_group_id" {
  description = "Security group ID attached to the EKS cluster"
  value       = module.eks.cluster_security_group_id
}

output "cluster_security_group_arn" {
  description = "ARN of the security group attached to the EKS cluster"
  value       = module.eks.cluster_security_group_arn
}

output "cluster_iam_role_name" {
  description = "IAM role name associated with EKS cluster"
  value       = module.eks.cluster_iam_role_name
}

output "cluster_iam_role_arn" {
  description = "IAM role ARN associated with EKS cluster"
  value       = module.eks.cluster_iam_role_arn
}

output "cluster_certificate_authority_data" {
  description = "Base64 encoded certificate data required to communicate with the cluster"
  value       = module.eks.cluster_certificate_authority_data
}

output "cluster_primary_security_group_id" {
  description = "The cluster primary security group ID created by EKS"
  value       = module.eks.cluster_primary_security_group_id
}

output "eks_managed_node_groups" {
  description = "Map of attribute maps for all EKS managed node groups created"
  value       = module.eks.eks_managed_node_groups
  sensitive   = true
}

# RDS Outputs
output "rds_instance_address" {
  description = "RDS instance hostname"
  value       = module.rds.db_instance_address
  sensitive   = true
}

output "rds_instance_arn" {
  description = "RDS instance ARN"
  value       = module.rds.db_instance_arn
}

output "rds_instance_availability_zone" {
  description = "RDS instance availability zone"
  value       = module.rds.db_instance_availability_zone
}

output "rds_instance_endpoint" {
  description = "RDS instance endpoint"
  value       = module.rds.db_instance_endpoint
  sensitive   = true
}

output "rds_instance_hosted_zone_id" {
  description = "RDS instance hosted zone ID"
  value       = module.rds.db_instance_hosted_zone_id
}

output "rds_instance_id" {
  description = "RDS instance ID"
  value       = module.rds.db_instance_id
}

output "rds_instance_resource_id" {
  description = "RDS instance resource ID"
  value       = module.rds.db_instance_resource_id
}

output "rds_instance_status" {
  description = "RDS instance status"
  value       = module.rds.db_instance_status
}

output "rds_instance_name" {
  description = "RDS instance name"
  value       = module.rds.db_instance_name
}

output "rds_instance_username" {
  description = "RDS instance root username"
  value       = module.rds.db_instance_username
  sensitive   = true
}

output "rds_instance_password" {
  description = "RDS instance master password"
  value       = module.rds.db_master_password
  sensitive   = true
}

output "rds_instance_port" {
  description = "RDS instance port"
  value       = module.rds.db_instance_port
}

output "rds_db_subnet_group_name" {
  description = "RDS subnet group name"
  value       = module.rds.db_subnet_group_name
}

output "rds_db_parameter_group_name" {
  description = "RDS parameter group name"
  value       = module.rds.db_parameter_group_name
}

output "rds_db_option_group_name" {
  description = "RDS option group name"
  value       = module.rds.db_option_group_name
}

# ElastiCache Outputs
output "redis_id" {
  description = "ID of the ElastiCache replication group"
  value       = aws_elasticache_replication_group.redis.id
}

output "redis_arn" {
  description = "ARN of the created ElastiCache replication group"
  value       = aws_elasticache_replication_group.redis.arn
}

output "redis_engine_version_actual" {
  description = "Running version of the cache engine"
  value       = aws_elasticache_replication_group.redis.engine_version_actual
}

output "redis_cluster_enabled" {
  description = "Indicates if cluster mode is enabled"
  value       = aws_elasticache_replication_group.redis.cluster_enabled
}

output "redis_configuration_endpoint_address" {
  description = "Address of the replication group configuration endpoint when cluster mode is enabled"
  value       = aws_elasticache_replication_group.redis.configuration_endpoint_address
}

output "redis_primary_endpoint_address" {
  description = "Address of the endpoint for the primary node in the replication group"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
  sensitive   = true
}

output "redis_reader_endpoint_address" {
  description = "Address of the endpoint for the reader node in the replication group"
  value       = aws_elasticache_replication_group.redis.reader_endpoint_address
  sensitive   = true
}

output "redis_member_clusters" {
  description = "Identifiers of all the nodes that are part of this replication group"
  value       = aws_elasticache_replication_group.redis.member_clusters
}

output "redis_port" {
  description = "Redis port"
  value       = aws_elasticache_replication_group.redis.port
}

output "redis_auth_token" {
  description = "Redis AUTH token"
  value       = random_password.redis_auth.result
  sensitive   = true
}

# MSK Outputs
output "kafka_arn" {
  description = "Amazon Resource Name (ARN) of the MSK cluster"
  value       = aws_msk_cluster.kafka.arn
}

output "kafka_bootstrap_brokers" {
  description = "Comma separated list of one or more hostname:port pairs of kafka brokers suitable for bootstrapping connectivity to the kafka cluster"
  value       = aws_msk_cluster.kafka.bootstrap_brokers
  sensitive   = true
}

output "kafka_bootstrap_brokers_tls" {
  description = "One or more DNS names (or IP addresses) and TLS port pairs"
  value       = aws_msk_cluster.kafka.bootstrap_brokers_tls
  sensitive   = true
}

output "kafka_current_version" {
  description = "Current version of the MSK Cluster used for updates"
  value       = aws_msk_cluster.kafka.current_version
}

output "kafka_encryption_at_rest_kms_key_id" {
  description = "The ARN of the KMS key used to encrypt data at rest in the broker data volumes"
  value       = aws_msk_cluster.kafka.encryption_info[0].encryption_at_rest_kms_key_id
}

output "kafka_zookeeper_connect_string" {
  description = "A comma separated list of one or more hostname:port pairs to use to connect to the Apache Zookeeper cluster"
  value       = aws_msk_cluster.kafka.zookeeper_connect_string
  sensitive   = true
}

# S3 Outputs
output "s3_logs_bucket_id" {
  description = "Name of the S3 bucket for logs"
  value       = var.environment == "prod" ? aws_s3_bucket.logs[0].id : null
}

output "s3_logs_bucket_arn" {
  description = "ARN of the S3 bucket for logs"
  value       = var.environment == "prod" ? aws_s3_bucket.logs[0].arn : null
}

output "s3_logs_bucket_domain_name" {
  description = "Domain name of the S3 bucket for logs"
  value       = var.environment == "prod" ? aws_s3_bucket.logs[0].bucket_domain_name : null
}

output "s3_assets_bucket_id" {
  description = "Name of the S3 bucket for assets"
  value       = aws_s3_bucket.assets.id
}

output "s3_assets_bucket_arn" {
  description = "ARN of the S3 bucket for assets"
  value       = aws_s3_bucket.assets.arn
}

output "s3_assets_bucket_domain_name" {
  description = "Domain name of the S3 bucket for assets"
  value       = aws_s3_bucket.assets.bucket_domain_name
}

output "s3_backups_bucket_id" {
  description = "Name of the S3 bucket for backups"
  value       = aws_s3_bucket.backups.id
}

output "s3_backups_bucket_arn" {
  description = "ARN of the S3 bucket for backups"
  value       = aws_s3_bucket.backups.arn
}

output "s3_backups_bucket_domain_name" {
  description = "Domain name of the S3 bucket for backups"
  value       = aws_s3_bucket.backups.bucket_domain_name
}

# Load Balancer Outputs
output "alb_id" {
  description = "ID of the Application Load Balancer"
  value       = aws_lb.main.id
}

output "alb_arn" {
  description = "ARN of the Application Load Balancer"
  value       = aws_lb.main.arn
}

output "alb_arn_suffix" {
  description = "ARN suffix for use with CloudWatch Metrics"
  value       = aws_lb.main.arn_suffix
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "alb_canonical_hosted_zone_id" {
  description = "Canonical hosted zone ID of the Application Load Balancer"
  value       = aws_lb.main.zone_id
}

output "alb_internal_id" {
  description = "ID of the Internal Application Load Balancer"
  value       = aws_lb.internal.id
}

output "alb_internal_arn" {
  description = "ARN of the Internal Application Load Balancer"
  value       = aws_lb.internal.arn
}

output "alb_internal_dns_name" {
  description = "DNS name of the Internal Application Load Balancer"
  value       = aws_lb.internal.dns_name
}

output "alb_internal_canonical_hosted_zone_id" {
  description = "Canonical hosted zone ID of the Internal Application Load Balancer"
  value       = aws_lb.internal.zone_id
}

# Security Group Outputs
output "rds_security_group_id" {
  description = "ID of the RDS security group"
  value       = aws_security_group.rds.id
}

output "redis_security_group_id" {
  description = "ID of the Redis security group"
  value       = aws_security_group.redis.id
}

output "msk_security_group_id" {
  description = "ID of the MSK security group"
  value       = aws_security_group.msk.id
}

output "eks_nodes_security_group_id" {
  description = "ID of the EKS nodes security group"
  value       = aws_security_group.eks_nodes.id
}

output "alb_security_group_id" {
  description = "ID of the ALB security group"
  value       = aws_security_group.alb.id
}

output "alb_internal_security_group_id" {
  description = "ID of the internal ALB security group"
  value       = aws_security_group.alb_internal.id
}

# Certificate Manager Outputs
output "acm_certificate_arn" {
  description = "ARN of the certificate"
  value       = var.domain_name != "" ? aws_acm_certificate.main[0].arn : null
}

output "acm_certificate_domain_validation_options" {
  description = "Set of domain validation objects which can be used to complete certificate validation"
  value       = var.domain_name != "" ? aws_acm_certificate.main[0].domain_validation_options : null
}

output "acm_certificate_status" {
  description = "Status of the certificate"
  value       = var.domain_name != "" ? aws_acm_certificate.main[0].status : null
}

# Route53 Outputs
output "route53_zone_id" {
  description = "Zone ID of Route53 zone"
  value       = var.domain_name != "" ? data.aws_route53_zone.main[0].zone_id : null
}

output "route53_zone_name_servers" {
  description = "Name servers of Route53 zone"
  value       = var.domain_name != "" ? data.aws_route53_zone.main[0].name_servers : null
}

output "route53_api_record_name" {
  description = "Name of the API Route53 record"
  value       = var.domain_name != "" ? aws_route53_record.api[0].name : null
}

output "route53_api_record_fqdn" {
  description = "FQDN built using the zone domain and name"
  value       = var.domain_name != "" ? aws_route53_record.api[0].fqdn : null
}

output "route53_admin_record_name" {
  description = "Name of the Admin Route53 record"
  value       = var.domain_name != "" ? aws_route53_record.admin[0].name : null
}

output "route53_admin_record_fqdn" {
  description = "FQDN built using the zone domain and name"
  value       = var.domain_name != "" ? aws_route53_record.admin[0].fqdn : null
}

# KMS Outputs
output "kms_key_id" {
  description = "ID of the KMS key for MSK"
  value       = aws_kms_key.msk.key_id
}

output "kms_key_arn" {
  description = "ARN of the KMS key for MSK"
  value       = aws_kms_key.msk.arn
}

output "kms_alias_arn" {
  description = "ARN of the KMS key alias for MSK"
  value       = aws_kms_alias.msk.arn
}

# IAM Role Outputs
output "eks_service_role_arn" {
  description = "ARN of the EKS service role"
  value       = aws_iam_role.eks_service_role.arn
}

output "eks_node_group_role_arn" {
  description = "ARN of the EKS node group role"
  value       = aws_iam_role.eks_node_group_role.arn
}

# CloudWatch Outputs
output "redis_cloudwatch_log_group_name" {
  description = "Name of the Redis CloudWatch log group"
  value       = aws_cloudwatch_log_group.redis.name
}

output "msk_cloudwatch_log_group_name" {
  description = "Name of the MSK CloudWatch log group"
  value       = aws_cloudwatch_log_group.msk.name
}