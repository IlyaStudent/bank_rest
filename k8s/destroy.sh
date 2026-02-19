#!/bin/bash
set -e

echo "=========================================="
echo "  Destroying Bank REST namespace..."
echo "=========================================="

kubectl delete namespace bank-rest --ignore-not-found=true

echo ""
echo "Done! All resources in namespace 'bank-rest' have been deleted."