locals {
  # This is the convention we use to know what belongs to each other
  ecs_resources_name = terraform.workspace == "default" ? "svc-${local.name}-${local.environment}" : "svc-${local.name}-${local.environment}-${terraform.workspace}"
}

resource "aws_sfn_activity" "forge_ecs_task" {
  name = "${local.ecs_resources_name}-ecs-activity"
  tags = merge(var.default_tags, { Project = var.prefix })
}

module "forge_fargate" {

  count = var.forge_fargate ? 1 : 0

  source = "./fargate"

  prefix = var.prefix
  app_name = var.app_name
  tags = var.default_tags
  iam_role = var.fargate_iam_role
  command = [
    "java",
    "-jar",
    "/home/dockeruser/build/libs/shadow.jar",
    aws_sfn_activity.forge_ecs_task.id
  ]

  environment = {
    "CONFIG_BUCKET": "${var.prefix}-internal",
    "CONFIG_DIR" : var.config_dir,
    "CONFIG_URL" : var.config_url,
    "AWS_DEFAULT_REGION" : var.region,
    "SOCKET_TIMEOUT" : var.socket_timeout,
    "REGION" : var.region,
    "FOOTPRINT_OUTPUT_BUCKET" : var.footprint_output_bucket,
    "FOOTPRINT_OUTPUT_DIR": var.footprint_output_dir
  }

  image = var.image
  ecs_cluster_arn = var.cluster_arn
  subnet_ids = var.subnet_ids
  scale_dimensions =  var.scale_dimensions != null ? var.scale_dimensions : {"ServiceName" = "${var.prefix}-${var.app_name}-fargate-service","ClusterName" = var.ecs_cluster_name}

  cpu = var.fargate_cpu
  memory = var.fargate_memory
  cluster_name = var.ecs_cluster_name

  desired_count = var.fargate_desired_count
  max_capacity = var.fargate_max_capacity
  min_capacity = var.fargate_min_capacity

  scale_up_cooldown = var.scale_up_cooldown
  scale_down_cooldown = var.scale_down_cooldown

  # Scale up settings
  comparison_operator_scale_up = var.comparison_operator_scale_up
  evaluation_periods_scale_up = var.evaluation_periods_scale_up
  metric_name_scale_up = var.metric_name_scale_up
  namespace_scale_up = var.namespace_scale_up
  period_scale_up = var.period_scale_up
  statistic_scale_up = var.statistic_scale_up
  threshold_scale_up = var.threshold_scale_up
  scale_up_step_adjustment = var.scale_up_step_adjustment

  # Scale down settings
  comparison_operator_scale_down = var.comparison_operator_scale_down
  evaluation_periods_scale_down = var.evaluation_periods_scale_down
  metric_name_scale_down = var.metric_name_scale_down
  namespace_scale_down = var.namespace_scale_down
  period_scale_down = var.period_scale_down
  statistic_scale_down = var.statistic_scale_down
  threshold_scale_down = var.threshold_scale_down
  scale_down_step_adjustment = var.scale_down_step_adjustment
}
