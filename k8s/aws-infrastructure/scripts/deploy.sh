#!/bin/bash

# Deploy script for Zendapag AWS Infrastructure
# Usage: ./deploy.sh <environment> [action]
# Example: ./deploy.sh staging apply
# Example: ./deploy.sh prod plan

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="${SCRIPT_DIR}/../terraform"
ENVIRONMENTS=("dev" "staging" "prod")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 <environment> [action]"
    echo ""
    echo "Environments: ${ENVIRONMENTS[*]}"
    echo "Actions:"
    echo "  init     - Initialize Terraform (default if no tfstate found)"
    echo "  plan     - Show execution plan"
    echo "  apply    - Apply changes (default action)"
    echo "  destroy  - Destroy infrastructure (use with caution!)"
    echo "  output   - Show output values"
    echo "  validate - Validate Terraform configuration"
    echo "  fmt      - Format Terraform files"
    echo ""
    echo "Examples:"
    echo "  $0 staging plan"
    echo "  $0 prod apply"
    echo "  $0 dev output"
    echo ""
    echo "Environment Variables:"
    echo "  TF_VAR_domain_name  - Domain name (optional)"
    echo "  AWS_PROFILE         - AWS profile to use"
    echo "  AWS_REGION          - AWS region (default: us-west-2)"
}

# Function to validate environment
validate_environment() {
    local env=$1
    if [[ ! " ${ENVIRONMENTS[@]} " =~ " ${env} " ]]; then
        print_error "Invalid environment: ${env}"
        print_info "Valid environments: ${ENVIRONMENTS[*]}"
        exit 1
    fi
}

# Function to check prerequisites
check_prerequisites() {
    local commands=("terraform" "aws" "jq")

    for cmd in "${commands[@]}"; do
        if ! command -v "$cmd" &> /dev/null; then
            print_error "Required command '$cmd' not found. Please install it first."
            exit 1
        fi
    done

    # Check Terraform version
    local tf_version=$(terraform version -json | jq -r '.terraform_version')
    local min_version="1.0.0"

    if ! printf '%s\n%s\n' "$min_version" "$tf_version" | sort -V -C; then
        print_error "Terraform version $tf_version is too old. Minimum required: $min_version"
        exit 1
    fi

    print_success "Prerequisites check passed"
}

# Function to set AWS credentials and region
setup_aws() {
    local environment=$1

    # Set default region if not specified
    export AWS_REGION=${AWS_REGION:-"us-west-2"}

    # Set AWS profile based on environment if not already set
    if [[ -z "${AWS_PROFILE:-}" ]]; then
        case $environment in
            "prod")
                export AWS_PROFILE="zendapag-prod"
                ;;
            "staging")
                export AWS_PROFILE="zendapag-staging"
                ;;
            "dev")
                export AWS_PROFILE="zendapag-dev"
                ;;
        esac
    fi

    print_info "Using AWS Profile: ${AWS_PROFILE:-default}"
    print_info "Using AWS Region: ${AWS_REGION}"

    # Verify AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        print_error "AWS credentials not properly configured"
        print_info "Please configure AWS credentials using:"
        print_info "  aws configure --profile \${AWS_PROFILE}"
        print_info "or set environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY"
        exit 1
    fi

    local account_id=$(aws sts get-caller-identity --query Account --output text)
    local user_arn=$(aws sts get-caller-identity --query Arn --output text)
    print_success "AWS credentials verified for account: $account_id"
    print_info "Identity: $user_arn"
}

# Function to initialize Terraform
terraform_init() {
    local environment=$1

    print_info "Initializing Terraform for environment: $environment"

    cd "$TERRAFORM_DIR"

    # Create terraform.tfvars if it doesn't exist
    if [[ ! -f "terraform.tfvars" ]]; then
        cat > terraform.tfvars <<EOF
# Terraform variables for $environment environment
environment = "$environment"
aws_region  = "${AWS_REGION:-us-west-2}"

# Uncomment and set domain name if using custom domain
# domain_name = "yourdomain.com"

# VPC Configuration
# vpc_cidr = "10.0.0.0/16"

# Database Configuration
# rds_instance_class = "db.r6g.large"
# rds_allocated_storage = 100

# Cache Configuration
# redis_node_type = "cache.r6g.medium"

# Kafka Configuration
# kafka_instance_type = "kafka.m5.large"
# kafka_ebs_volume_size = 100
EOF
        print_success "Created terraform.tfvars template"
        print_warning "Please review and update terraform.tfvars with your desired configuration"
    fi

    # Initialize Terraform
    terraform init \
        -backend-config="bucket=zendapag-terraform-state" \
        -backend-config="key=infrastructure/${environment}/terraform.tfstate" \
        -backend-config="region=${AWS_REGION:-us-west-2}" \
        -backend-config="encrypt=true" \
        -backend-config="dynamodb_table=zendapag-terraform-locks"

    print_success "Terraform initialization completed"
}

# Function to run Terraform plan
terraform_plan() {
    local environment=$1

    print_info "Running Terraform plan for environment: $environment"

    cd "$TERRAFORM_DIR"

    terraform plan \
        -var="environment=$environment" \
        -var="aws_region=${AWS_REGION:-us-west-2}" \
        -out="$environment.tfplan"

    print_success "Terraform plan completed. Plan saved to $environment.tfplan"
    print_warning "Please review the plan carefully before applying"
}

# Function to run Terraform apply
terraform_apply() {
    local environment=$1
    local plan_file="$environment.tfplan"

    cd "$TERRAFORM_DIR"

    if [[ -f "$plan_file" ]]; then
        print_info "Applying Terraform plan from $plan_file"
        terraform apply "$plan_file"
        rm -f "$plan_file"
    else
        print_info "No plan file found. Running apply directly for environment: $environment"
        print_warning "This will show the plan and ask for confirmation"

        terraform apply \
            -var="environment=$environment" \
            -var="aws_region=${AWS_REGION:-us-west-2}"
    fi

    print_success "Terraform apply completed"

    # Show important outputs
    print_info "Important connection information:"
    terraform output -json | jq -r '
        to_entries[] |
        select(.key | test("endpoint|dns_name|bootstrap_brokers")) |
        "\(.key): \(.value.value // "null")"
    '
}

# Function to run Terraform destroy
terraform_destroy() {
    local environment=$1

    print_warning "WARNING: This will destroy all infrastructure for environment: $environment"
    print_warning "This action cannot be undone!"

    read -p "Are you sure you want to destroy the infrastructure? Type 'yes' to confirm: " -r
    if [[ ! $REPLY == "yes" ]]; then
        print_info "Operation cancelled"
        exit 0
    fi

    if [[ "$environment" == "prod" ]]; then
        print_warning "PRODUCTION ENVIRONMENT DETECTED!"
        read -p "Type the environment name '$environment' to confirm destruction: " -r
        if [[ ! $REPLY == "$environment" ]]; then
            print_error "Environment name mismatch. Operation cancelled for safety"
            exit 1
        fi
    fi

    cd "$TERRAFORM_DIR"

    terraform destroy \
        -var="environment=$environment" \
        -var="aws_region=${AWS_REGION:-us-west-2}"

    print_success "Terraform destroy completed"
}

# Function to show Terraform outputs
terraform_output() {
    local environment=$1

    print_info "Terraform outputs for environment: $environment"

    cd "$TERRAFORM_DIR"

    if terraform output -json &> /dev/null; then
        terraform output -json | jq '.'
    else
        print_warning "No outputs available. Infrastructure may not be deployed yet."
    fi
}

# Function to validate Terraform configuration
terraform_validate() {
    print_info "Validating Terraform configuration"

    cd "$TERRAFORM_DIR"

    terraform fmt -check -diff -recursive .
    terraform validate

    print_success "Terraform validation completed"
}

# Function to format Terraform files
terraform_format() {
    print_info "Formatting Terraform files"

    cd "$TERRAFORM_DIR"

    terraform fmt -recursive .

    print_success "Terraform formatting completed"
}

# Function to setup backend (run once per AWS account)
setup_backend() {
    local region=${AWS_REGION:-"us-west-2"}

    print_info "Setting up Terraform backend resources"

    # Create S3 bucket for state
    if ! aws s3 ls "s3://zendapag-terraform-state" &> /dev/null; then
        print_info "Creating S3 bucket for Terraform state"
        aws s3 mb "s3://zendapag-terraform-state" --region "$region"

        # Enable versioning
        aws s3api put-bucket-versioning \
            --bucket zendapag-terraform-state \
            --versioning-configuration Status=Enabled

        # Enable server-side encryption
        aws s3api put-bucket-encryption \
            --bucket zendapag-terraform-state \
            --server-side-encryption-configuration '{
                "Rules": [
                    {
                        "ApplyServerSideEncryptionByDefault": {
                            "SSEAlgorithm": "AES256"
                        }
                    }
                ]
            }'

        # Block public access
        aws s3api put-public-access-block \
            --bucket zendapag-terraform-state \
            --public-access-block-configuration \
                BlockPublicAcls=true,\
                IgnorePublicAcls=true,\
                BlockPublicPolicy=true,\
                RestrictPublicBuckets=true
    fi

    # Create DynamoDB table for locks
    if ! aws dynamodb describe-table --table-name zendapag-terraform-locks &> /dev/null; then
        print_info "Creating DynamoDB table for Terraform locks"
        aws dynamodb create-table \
            --table-name zendapag-terraform-locks \
            --attribute-definitions AttributeName=LockID,AttributeType=S \
            --key-schema AttributeName=LockID,KeyType=HASH \
            --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
            --region "$region"

        # Wait for table to be active
        aws dynamodb wait table-exists --table-name zendapag-terraform-locks --region "$region"
    fi

    print_success "Backend setup completed"
}

# Main function
main() {
    if [[ $# -lt 1 ]]; then
        show_usage
        exit 1
    fi

    local environment=$1
    local action=${2:-"apply"}

    # Special case for setup-backend
    if [[ "$environment" == "setup-backend" ]]; then
        check_prerequisites
        setup_aws "dev"  # Use dev for setup
        setup_backend
        exit 0
    fi

    # Validate environment
    validate_environment "$environment"

    # Check prerequisites
    check_prerequisites

    # Setup AWS credentials
    setup_aws "$environment"

    # Handle different actions
    case $action in
        "init")
            terraform_init "$environment"
            ;;
        "plan")
            terraform_init "$environment"
            terraform_plan "$environment"
            ;;
        "apply")
            terraform_init "$environment"
            terraform_plan "$environment"
            terraform_apply "$environment"
            ;;
        "destroy")
            terraform_init "$environment"
            terraform_destroy "$environment"
            ;;
        "output")
            terraform_output "$environment"
            ;;
        "validate")
            terraform_validate
            ;;
        "fmt")
            terraform_format
            ;;
        *)
            print_error "Unknown action: $action"
            show_usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"