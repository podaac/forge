import sys
import json

if __name__ == '__main__':

    data = {'module': [{'forge_module': {'source': sys.argv[1], 'image': sys.argv[2]}}]}
    with open('override.tf.json', 'w') as f:
        json.dump(data, f)


