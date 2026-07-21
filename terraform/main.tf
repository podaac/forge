locals {
  name = var.app_name
  environment = var.prefix

  tags = {
    Deployment = var.prefix
  }
}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
    }
    null = {
      source  = "hashicorp/null"
    }
  }
}

locals {
  default_tags = length(var.default_tags) == 0 ? {
    team: "PODAAC TVA",
    application: var.app_name,
  } : var.default_tags
}