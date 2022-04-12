# gh-contributors-cli

This was originally a Babashka script created [here](https://gist.github.com/cesarcneto/f25207f22e6e49d22df011169fb47a49) to help hiring managers to reach out to OpenSource contributors.

It fetches contributors from a given Github repo, fetches their user profile plus most recent git commit of each user.

## Requirements

To make use of this CLI you're expected to have installed the following softwares:
TBD

## Quickstart

Once you have the required software installed (see #requirements), you got follow a couple of steps:

### 1 - Create your personal access token in Github

[Create your personal access token in Github](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)

Note: This is a required step in order to run the script without being rate limitted.

### 2 - Make the required environment variables available

2 - Make `GH_FETCH_CONTRIBUTORS_USER` and `GH_FETCH_CONTRIBUTORS_TOKEN` environment variables available

E.g. in a terminal window run:
```bash
export GH_FETCH_CONTRIBUTORS_USER=<YOUR_GITHUB_USER_NAME_HERE>
export GH_FETCH_CONTRIBUTORS_TOKEN=<YOUR_GITHUB_ACCESS_TOKNE_HERE>
```

### 3 - Run the program

#### 3.a - Using leiningen

In the same terminal window that you ran step #2, run:

```bash
lein run gh-contributors-cli.main --repo apache/apark
```

#### 3.b - Using a jar file

There are two options to make use of a jar file.
1. Download the latest release [here](https://github.com/cesarcneto/gh-contributors-cli/releases/download/v0.1.0-snapshot/gh-contributors-cli-0.1.0-SNAPSHOT.jar); or
2. Download it in your terminal using `curl -L -o <your-local-file-name-and-destionation> https://github.com/cesarcneto/gh-contributors-cli/releases/download/v0.1.0-snapshot/gh-contributors-cli-0.1.0-SNAPSHOT.jar`

In the same terminal window that you ran step #2, run:

```bash
java -jar gh-contributors-cli-latest.jar --repo apache/spark
```

Once the command run is completed, a file called `result.csv` should be created at `.data/apache/spark` (assuming that you provided `--repo apache/spark` to the script). From this point, you should be able to import this csv file into your favorite spreadsheet tool and have fun! ^.^

**IMPORTANT NOTE**: This script will keep data cached in `.data` folder. Make sure you delete the folder whenever you want to fetch data for a given repo again.
