#### classpath-folder

Reads configuration JsonObject from files in folder on classpath, merges them and puts in output config at `outputPath`.

Example store configuration:

```json
{
  "type": "classpath-folder",
  "format": "json",
  "config": {
    "path": "config",
    "filesets": [
        {"pattern": "*json"},
        {"pattern": "*.properties"}
    ],
    "outputPath": "some.output.path"
  }
}
```