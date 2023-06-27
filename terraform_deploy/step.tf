resource "aws_sfn_state_machine" "sfn_state_machine" {
  name     = "${var.prefix}-forge"
  role_arn = aws_iam_role.step.arn

  definition = <<EOF
{
  "Comment": "Footprint Processing",
  "StartAt": "FootprintProcess",
  "States": {
    "FootprintProcess": {
      "Parameters": {
        "cma": {
          "event.$": "$",
          "task_config": {
            "collection": "{$.meta.collection}",
            "cumulus_message": {
              "input": "{$.payload}",
              "outputs": [
                {
                  "source": "{$.input.granules}",
                  "destination": "{$.payload.granules}"
                }
              ]
            }
          }
        }
      },
      "Type": "Task",
      "Resource": "${module.forge_module.forge_task_arn}",
      "Catch": [
        {
          "ErrorEquals": [
            "States.ALL"
          ],
          "ResultPath": "$.exception",
          "Next": "WorkflowFailed"
        }
      ],
      "Retry": [
        {
          "ErrorEquals": [
            "States.ALL"
          ],
          "IntervalSeconds": 2,
          "MaxAttempts": 1
        }
      ],
      "Next": "WorkflowSucceeded"
    },
    "WorkflowSucceeded": {
      "Type": "Succeed"
    },
    "WorkflowFailed": {
      "Type": "Fail",
      "Cause": "Workflow failed"
    }
  }
}
EOF
}