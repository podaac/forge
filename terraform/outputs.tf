output "forge_task_arn" {
  value = aws_lambda_function.forge_task.arn
}

output "forge_function_name" {
  value = aws_lambda_function.forge_task.function_name
}

output "forge_ecs_task_id" {
  value = aws_sfn_activity.forge_ecs_task.id
}
