variable "aws_profile" {
  type    = string
  default = null
}

variable "prefix" {
  type = string
}

variable "cmr_environment" {
  type = string
}

# The location of footprint dataset-config file. Ex. s3://my-internal/datset-config/
variable "config_bucket" {
  type = string
  default = null
}

variable "config_dir" {
  type = string
  default = null
}

variable "config_url" {
  type = string
  default = null
}

variable "footprint_output_bucket" {
  type = string
}
variable "footprint_output_dir" {
  type = string
}

variable "lambda_role" {
  type = string
}

variable "layers" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "subnet_ids" {
  type = list(string)
}

variable "region" {
  type = string
  default = "us-west-2"
}

variable socket_timeout {
  description = "socket timeout for step function client"
  type = number
  default = 600
}

variable "app_name" {
  default = "forge"
}

variable "default_tags" {
  type = map(string)
  default = {}
}

variable "log_level" {
  type = string
  default = "Info"
}

variable "memory_size" {
  type = number
  default = 512
}

variable "timeout" {
  type = number
  default = 120
}

variable "profile" {
  type    = string
  default = null
}

# FARGATE variables

variable image{
  description = "ECR image arn"
  default = ""
  type = string
}

variable lambda_container_image_uri{
  description = "public image url"
  type = string
}

variable fargate_iam_role{
  description = "iam role to use for a fargate task"
  type = string
  default = ""
}

variable ecs_cluster_name{
  description = "cluster where fargate task will be deployed"
  type = string
  default = ""
}

variable forge_fargate{
  description = "boolean to deploy fargate task"
  type = bool
  default = false
}

variable fargate_memory{
  description = "amount of memory to allocate for a single fargate task"
  type = number
  default = 2048
}

variable fargate_cpu{
  description = "amount of cpu to allocate for a single fargate task"
  type = number
  default = 1024
}

variable fargate_desired_count{
  description = "desired count of how many fargate task"
  type = number
  default = 1
}

variable fargate_min_capacity{
  description = "minimum number of fargate task when scaling"
  type = number
  default = 1
}

variable fargate_max_capacity{
  description = "maximum number of fargate task when scaling"
  type = number
  default = 10
}

variable scale_dimensions{
  description = "cloudwatch dimensions to scale on"
  type = map(string)
  default = null
}

variable scale_up_cooldown{
  description = "seconds before able to scaling up again"
  type = number
  default = 60
}

variable scale_down_cooldown{
  description = "seconds before able to scaling down again"
  type = number
  default = 120
}

variable comparison_operator_scale_up{
  description = "The arithmetic operation to use when comparing the specified Statistic and Threshold"
  type = string
  default = "GreaterThanOrEqualToThreshold"
}

variable evaluation_periods_scale_up{
  description = "The number of periods over which data is compared to the specified threshold"
  type = number
  default = 1
}

variable metric_name_scale_up{
  description = "name of the metric"
  type = string
  default = "CPUUtilization"
}

variable namespace_scale_up{
  description = "namespace for the alarm's associated metric"
  type = string
  default = "AWS/ECS"
}

variable period_scale_up{
  description = "period in seconds over which the specified statistic is applied"
  type = number
  default = 60
}

variable statistic_scale_up{
  description = "statistic to apply to the metric"
  type = string
  default = "Average"
}

variable threshold_scale_up{
  description = "threshold for statistic to compare against to trigger step"
  type = number
  default = 50
}

variable scale_up_step_adjustment{
  description = "step adjustment to make when scaling up fargate"
  type = list
  default = [
    {
      metric_interval_lower_bound = 0
      metric_interval_upper_bound = ""
      scaling_adjustment = 1
    }
  ]
}

variable comparison_operator_scale_down{
  description = "The arithmetic operation to use when comparing the specified Statistic and Threshold"
  type = string
  default = "LessThanOrEqualToThreshold"
}

variable evaluation_periods_scale_down{
  description = "The number of periods over which data is compared to the specified threshold"
  type = number
  default = 1
}

variable metric_name_scale_down{
  description = "name of the metric"
  type = string
  default = "CPUUtilization"
}

variable namespace_scale_down{
  description = "namespace for the alarm's associated metric"
  type = string
  default = "AWS/ECS"
}

variable period_scale_down{
  description = "period in seconds over which the specified statistic is applied"
  type = number
  default = 60
}

variable statistic_scale_down{
  description = "statistic to apply to the metric"
  type = string
  default = "Average"
}

variable threshold_scale_down{
  description = "threshold for statistic to compare against to trigger step"
  type = number
  default = 50
}

# use "" to indicate infinity
variable "scale_down_step_adjustment" {
  description = "step adjustment to make when scaling down fargate"
  type = list
  default = [
    {
      metric_interval_lower_bound = ""
      metric_interval_upper_bound = 0
      scaling_adjustment = -1
    },
    {
      metric_interval_lower_bound = 0
      metric_interval_upper_bound = ""
      scaling_adjustment = 0
    }
  ]
}

variable cluster_arn{
  description = "cumulus cluster arn"
  type = string
  default = ""
}

