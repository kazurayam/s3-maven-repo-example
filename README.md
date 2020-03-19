# s3-maven-repo-example
This repository accompanies a blog post detailing how to set up and use an S3-based Maven repository with Gradle. For more information see [that post](https://medium.com/@JacobASeverson/s3-maven-repositories-and-gradle-911c25cebeeb)


# 追伸

サブプロジェクト `player-api` がAWS Lambdaで動くLambda関数としても動くように作りかえた。そしてjarファイルをS3バケット へアップロードする機能を追加した。

S3バケットの名前は `bb4b24b08c-20200319-artifacts` 。
以下でこのS3バケットのことを artifactsバケット と略記する。
artifactsという英単語は準公的生成物という意味で、つまるところjarファイルのことである。

まず準備。
Lambda関数を実装したjarファイルを格納するために artifactsバケット を作る
```
$ gradle :artifacts-bucket:create
```

artifactsバケットにバケットポリシーを付与する
```
$ gradle :artifacts-bucket:setBucketPolicy
```

つぎに
:player-apiプロジェクトのbuild/libsディレクトリからartifactsバケットへjarをアップロードしよう。

```
$ gradle :player-api:uploadArtifact
```

別解。:player-apiプロジェクトがjarファイルをMavenレポジトリへpublish済みであると前提して、Mavenレポジトリ上のjarファイルをartifactsバケットへ転送しよう。

```
$ gradle :player-api:transferArtifact
```
