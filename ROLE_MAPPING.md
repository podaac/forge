# Role Mapping for S3 Downloads

The Forge application now supports role assumption for S3 downloads when accessing buckets that require different credentials than the default Lambda/ECS execution role.

## Configuration

To enable role assumption for specific buckets, add a `role_mappings` object to your configuration. The mapping uses regex patterns to match bucket names:

```json
{
  "config": {
    "role_mappings": {
      "exact-bucket-name": "arn:aws:iam::123456789012:role/role-for-exact-bucket",
      ".*-protected": "arn:aws:iam::123456789012:role/protected-data-role",
      ".*-private": "arn:aws:iam::123456789012:role/private-data-role",
      "podaac-.*-internal": "arn:aws:iam::123456789012:role/internal-data-role"
    }
  }
}
```

## How it works

1. When downloading files from S3, the application checks if the bucket name matches any regex pattern in the `role_mappings` configuration
2. If a matching pattern is found, the application assumes the corresponding role using AWS STS (Security Token Service)
3. The S3 client is then created with the assumed role credentials
4. If no matching pattern is found for the bucket, the default credentials are used
5. The first matching pattern is used if multiple patterns could match a bucket name

## Example

```json
{
  "input": {
    "granules": [
      {
        "granuleId": "test-granule",
        "files": [
          {
            "bucket": "my-protected-data-bucket",
            "key": "path/to/file.nc",
            "fileName": "file.nc",
            "type": "data"
          }
        ]
      }
    ]
  },
  "config": {
    "role_mappings": {
      ".*-protected": "arn:aws:iam::123456789012:role/protected-data-access-role",
      "exact-bucket-name": "arn:aws:iam::123456789012:role/exact-bucket-role"
    },
    "collection": {
      "name": "test-collection"
    },
    "execution_name": "test-execution"
  }
}
```

In this example, when downloading from the `my-protected-data-bucket`, the application will match the `.*-protected` pattern and assume the `protected-data-access-role` before making the S3 request.

## Requirements

- The Lambda/ECS execution role must have permission to assume the target roles
- The target roles must have appropriate S3 permissions for the buckets they need to access
- The role ARNs must be valid and accessible from the current AWS account

## Regex Pattern Examples

Here are some common regex patterns you might use:

- `.*-protected` - Matches any bucket ending with "-protected"
- `.*-private` - Matches any bucket ending with "-private"
- `podaac-.*-internal` - Matches any bucket starting with "podaac-" and ending with "-internal"
- `exact-bucket-name` - Matches only the exact bucket name "exact-bucket-name"
- `.*-data-.*` - Matches any bucket containing "-data-" in the name

## Best Practices

1. **Order matters**: The first matching pattern will be used, so put more specific patterns before general ones
2. **Test your patterns**: Use a regex tester to verify your patterns match the expected bucket names
3. **Be specific**: Avoid overly broad patterns that might match unintended buckets
4. **Use exact matches**: For critical buckets, use exact bucket names rather than patterns

## Error Handling

- If role assumption fails, the application logs an error but continues with the default credentials
- If the target role doesn't exist or is not accessible, the download will fail with appropriate error messages
- If multiple patterns could match a bucket name, the first matching pattern in the configuration will be used 