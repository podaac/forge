locals {
  name = var.app_name
  environment = var.prefix

  tags = {
    Deployment = var.prefix
  }
}

terraform {
  required_providers {
    aws  = ">= 3.0"
    null = "~> 2.1"
  }
}

provider "aws" {
  region  = var.region
  profile = var.profile

  ignore_tags {
    key_prefixes = ["gsfc-ngap"]
  }
}

locals {
  default_tags = length(var.default_tags) == 0 ? {
    team: "PODAAC TVA",
    application: var.app_name,
  } : var.default_tags
}