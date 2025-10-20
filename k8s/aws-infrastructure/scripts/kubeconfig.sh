#!/bin/bash

# Script to configure kubectl for Zendapag EKS cluster
# Usage: ./kubeconfig.sh <environment> [region]

set -euo pipefail

# Configuration
ENVIRONMENTS=("dev" "staging" "prod")
DEFAULT_REGION="us-west-2"

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
    echo "Usage: $0 <environment> [region]"
    echo ""
    echo "Environments: ${ENVIRONMENTS[*]}"
    echo "Default region: $DEFAULT_REGION"
    echo ""
    echo "Examples:"
    echo "  $0 staging"
    echo "  $0 prod us-west-2"
    echo ""
    echo "This script will:"
    echo "  1. Configure kubectl to connect to the EKS cluster"
    echo "  2. Verify cluster connectivity"
    echo "  3. Show cluster information"
    echo ""
    echo "Environment Variables:"
    echo "  AWS_PROFILE - AWS profile to use"
    echo "  AWS_REGION  - AWS region override"
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
    local commands=("aws" "kubectl")

    for cmd in "${commands[@]}"; do
        if ! command -v "$cmd" &> /dev/null; then
            print_error "Required command '$cmd' not found. Please install it first."
            case $cmd in
                "aws")
                    print_info "Install AWS CLI: https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html"
                    ;;
                "kubectl")
                    print_info "Install kubectl: https://kubernetes.io/docs/tasks/tools/install-kubectl/"
                    ;;
            esac
            exit 1
        fi
    done

    print_success "Prerequisites check passed"
}

# Function to setup AWS profile
setup_aws() {
    local environment=$1

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
        exit 1
    fi

    local account_id=$(aws sts get-caller-identity --query Account --output text)
    local user_arn=$(aws sts get-caller-identity --query Arn --output text)
    print_success "AWS credentials verified for account: $account_id"
    print_info "Identity: $user_arn"
}

# Function to configure kubeconfig
configure_kubeconfig() {
    local environment=$1
    local region=$2
    local cluster_name="zendapag-${environment}-cluster"

    print_info "Configuring kubeconfig for cluster: $cluster_name"

    # Check if cluster exists
    if ! aws eks describe-cluster --name "$cluster_name" --region "$region" &> /dev/null; then
        print_error "EKS cluster '$cluster_name' not found in region '$region'"
        print_info "Please ensure the infrastructure is deployed and cluster name is correct"
        exit 1
    fi

    # Update kubeconfig
    aws eks update-kubeconfig \
        --region "$region" \
        --name "$cluster_name" \
        --alias "zendapag-${environment}"

    print_success "Kubeconfig updated successfully"
    print_info "Context name: zendapag-${environment}"
}

# Function to verify cluster connectivity
verify_connectivity() {
    local environment=$1

    print_info "Verifying cluster connectivity..."

    # Switch to the cluster context
    kubectl config use-context "zendapag-${environment}"

    # Test basic connectivity
    if ! kubectl get nodes &> /dev/null; then
        print_error "Failed to connect to the cluster"
        print_info "Please check your AWS credentials and cluster configuration"
        exit 1
    fi

    print_success "Successfully connected to the cluster"
}

# Function to show cluster information
show_cluster_info() {
    local environment=$1

    print_info "Cluster Information:"

    # Get cluster version
    local cluster_version=$(kubectl version --short --client=false 2>/dev/null | grep "Server Version" | awk '{print $3}' || echo "Unable to fetch")
    echo "  Kubernetes Version: $cluster_version"

    # Get node information
    local node_count=$(kubectl get nodes --no-headers | wc -l)
    echo "  Number of Nodes: $node_count"

    # Show nodes with their status
    echo ""
    print_info "Node Status:"
    kubectl get nodes -o wide

    # Show namespaces
    echo ""
    print_info "Available Namespaces:"
    kubectl get namespaces

    # Show system pods status
    echo ""
    print_info "System Pods Status:"
    kubectl get pods -n kube-system --field-selector=status.phase!=Succeeded -o wide

    # Check if Zendapag namespace exists
    if kubectl get namespace zendapag-$environment &> /dev/null; then
        echo ""
        print_info "Zendapag Application Pods:"
        kubectl get pods -n "zendapag-$environment" -o wide
    else
        echo ""
        print_warning "Zendapag namespace 'zendapag-$environment' not found"
        print_info "You may need to deploy the application first"
    fi

    # Show current context
    echo ""
    print_info "Current Context:"
    kubectl config current-context

    # Show cluster endpoint
    local cluster_endpoint=$(kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}')
    echo "  Cluster Endpoint: $cluster_endpoint"
}

# Function to install useful tools
install_tools() {
    print_info "Checking for useful Kubernetes tools..."

    # Check for kubectx and kubens
    if ! command -v kubectx &> /dev/null; then
        print_warning "kubectx not found. This tool helps switch between clusters easily."
        print_info "Install with: brew install kubectx (macOS) or check https://github.com/ahmetb/kubectx"
    fi

    # Check for k9s
    if ! command -v k9s &> /dev/null; then
        print_warning "k9s not found. This tool provides a terminal UI for Kubernetes."
        print_info "Install with: brew install k9s (macOS) or check https://github.com/derailed/k9s"
    fi

    # Check for helm
    if ! command -v helm &> /dev/null; then
        print_warning "helm not found. This is required for deploying Zendapag application."
        print_info "Install with: brew install helm (macOS) or check https://helm.sh/docs/intro/install/"
    fi
}

# Function to show helpful commands
show_helpful_commands() {
    local environment=$1

    echo ""
    print_info "Helpful Commands:"
    echo ""
    echo "# Switch to this cluster context:"
    echo "kubectl config use-context zendapag-${environment}"
    echo ""
    echo "# Get all resources in zendapag namespace:"
    echo "kubectl get all -n zendapag-${environment}"
    echo ""
    echo "# View logs from API pods:"
    echo "kubectl logs -l app.kubernetes.io/component=api -n zendapag-${environment} -f"
    echo ""
    echo "# View logs from Worker pods:"
    echo "kubectl logs -l app.kubernetes.io/component=worker -n zendapag-${environment} -f"
    echo ""
    echo "# Port forward to API service (for local testing):"
    echo "kubectl port-forward svc/zendapag-api 8080:8080 -n zendapag-${environment}"
    echo ""
    echo "# Execute shell in API pod:"
    echo "kubectl exec -it deployment/zendapag-api -n zendapag-${environment} -- /bin/bash"
    echo ""
    echo "# Scale deployments:"
    echo "kubectl scale deployment/zendapag-api --replicas=3 -n zendapag-${environment}"
    echo ""
    echo "# Check pod resource usage:"
    echo "kubectl top pods -n zendapag-${environment}"
    echo ""
    echo "# Deploy/upgrade application with Helm:"
    echo "helm upgrade --install zendapag ./k8s/helm/zendapag -n zendapag-${environment} --create-namespace"
}

# Main function
main() {
    if [[ $# -lt 1 ]]; then
        show_usage
        exit 1
    fi

    local environment=$1
    local region=${2:-$DEFAULT_REGION}

    # Set AWS region
    export AWS_REGION=$region

    # Validate environment
    validate_environment "$environment"

    # Check prerequisites
    check_prerequisites

    # Setup AWS credentials
    setup_aws "$environment"

    # Configure kubeconfig
    configure_kubeconfig "$environment" "$region"

    # Verify connectivity
    verify_connectivity "$environment"

    # Show cluster information
    show_cluster_info "$environment"

    # Check for useful tools
    install_tools

    # Show helpful commands
    show_helpful_commands "$environment"

    print_success "Setup completed! You can now interact with the $environment cluster."
}

# Run main function with all arguments
main "$@"