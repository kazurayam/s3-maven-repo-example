# s3-maven-repo-example
This repository accompanies a blog post detailing how to set up and use an S3-based Maven repository with Gradle. For more information see [that post](https://medium.com/@JacobASeverson/s3-maven-repositories-and-gradle-911c25cebeeb)


# 追伸

サブプロジェクト `player-api` が生成した `player-api-0.0.1.jar` ファイルを S3バケット へアップロードする機能を追加した。

S3バケットの名前は `bb4b24b08c-20200319-artifacts` 。
以下でこのS3バケットのことを artifactsバケット と略記する。
artifactsという英単語は準公的生成物という意味で、
つまるところjarファイルのことである。

Lambda関数を実装したjarファイルを格納するために artifactsバケット を作る
```
$ gradle :artifacts:create
```

:player-apiプロジェクトのbuild/libsディレクトリからlambdaバケットへjarをアップロードする

```
$ gradle :player-api:uploadArtifact
```

または :player-apiプロジェクトがjarファイルをMavenレポジトリへpublish済みであると前提したうえで、Mavenレポジトリ上のjarファイルをartifactsバケットへコピーする。

```
$ gradle :player:transferArtifact
```
