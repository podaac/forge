module "forge_module" {
    source = "source will be override by override.py"
    prefix = var.prefix
    region = var.region
    cmr_environment = "UAT"
    config_url = "https://hitide.podaac.earthdatacloud.nasa.gov/dataset-configs"
    footprint_output_bucket = "${var.prefix}-internal"
    footprint_output_dir    = "dataset-metadata"
    lambda_role = aws_iam_role.iam_execution.arn
    layers = [aws_lambda_layer_version.cumulus_message_adapter.arn]
    security_group_ids = [aws_security_group.lambda_sg.id]
    subnet_ids = data.aws_subnets.private.ids
    app_name = "${var.prefix}-forgeApp"
    memory_size = 512
    timeout = 600

    default_tags = local.default_tags
    profile = var.profile

    # ECS Variables
    cluster_arn = aws_ecs_cluster.main.arn

    # Fargate Variables
    forge_fargate = true
    fargate_memory = 512
    fargate_cpu = 256
    fargate_iam_role = aws_iam_role.fargate_execution.arn
    ecs_cluster_name = aws_ecs_cluster.main.name
    lambda_container_image_uri = "source will be override by override.py"
}