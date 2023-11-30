
resource "aws_lambda_function" "forge_task" {
  filename      = "${path.module}/forge-lambda.zip"
  function_name = "${var.prefix}-forge"
  source_code_hash = filebase64sha256("${path.module}/forge-lambda.zip")
  handler       = "gov.nasa.podaac.forge.FootprintHandler::handleRequestStreams"
  role          = var.lambda_role
  runtime       = "java11"
  timeout       = var.timeout
  memory_size   = var.memory_size

  layers = var.layers

  environment {
    variables = {
      STACK_NAME                  = var.prefix
      CMR_ENVIRONMENT             = var.cmr_environment
      CUMULUS_MESSAGE_ADAPTER_DIR = "/opt/"
      REGION                      = var.region
      CONFIG_BUCKET               = var.config_bucket
      CONFIG_DIR                  = var.config_dir
      FOOTPRINT_OUTPUT_BUCKET     = var.footprint_output_bucket
      FOOTPRINT_OUTPUT_DIR        = var.footprint_output_dir
      CONFIG_URL                  = var.config_url
      LOGGING_LEVEL               = var.log_level
    }
  }

  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = var.security_group_ids
  }

  tags = local.default_tags
}
