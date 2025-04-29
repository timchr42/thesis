import json
import os
import sys

if __name__ == "__main__":
    print("Cheers")
    if len(sys.argv) != 2:
        print("Generate json file for Beer-et-al pipeline from a folder of apks")
        print("Usage: python folder2json.py <path/to/apks>")
        exit(1)
    path = sys.argv[1]
    if not os.path.isdir(path):
        print("Invalid path, not a folder")
        exit(1)
    files = os.listdir(path)
    apps = [file.removesuffix(".apk") for file in files]
    result = {}
    for app in apps:
        result[app] = {}
        result[app]["mode"] = "SINGLE_APK"

    with open("apps.json", "w") as f:
        f.write(json.dumps(result))
    print("JSON exported.")
