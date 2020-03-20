# s3-maven-repo-example

This repository accompanies a blog post detailing how to set up and use an S3-based Maven repository with Gradle. For more information see [that post](https://medium.com/@JacobASeverson/s3-maven-repositories-and-gradle-911c25cebeeb)


# kazurayamによる説明

## 解決すべき問題

Java/Groovy/Kotlin言語によるソフトウエア開発プロジェクトの成果物はjarファイルの形をとる。役に立つライブラリのjarファイルをネットワーク上の[Mavenレポジトリ](https://ja.wikipedia.org/wiki/Apache_Maven)に格納して他のプロジェクトで再利用するのが標準的なやり方だ。Mavenレポジトリの実例としては有名な[Maven Central](https://mvnrepository.com/repos/central)レポジトリや[jCenter](https://mvnrepository.com/repos/jcenter)レポジトリがあるが、これらはインターネット上で限定なしに公開されたレポジトリだ。個人や企業が開発したライブラリを内輪に限定して公開したい場合、パブリックなMavenレポジトリを使うわけにはいかない。プライベートなネットワーク上にプライベートなMavenレポジトリを構築し内輪限りで利用したくなる。プライベートなMavenレポジトリを構築するのには[Artifactory]()というプロダクトがある。だがArtifactoryはjCenterに対抗しようという魂胆があるのか、けっこう複雑なソフトウェアであって学習コストがかかる。わたしとしてはもっとシンプルな方法で自分専用のMavenレポジトリを構築する方法はないものか？と考えてググったら、 **AWS S3のバケットを使ってプライベートなMavenレポジトリを作ったよ的な記事がいくつもあった**。

それらの記事はたいていビルドツール[Gradle](https://gradle.org/)に[Maven Publish plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)を組み込んで、AWS S3バケットをMavenレポジトリとして使うという方法をとっていた。この方法があるから少しも難しくはない。しかしひとつ問題があった。パソコン上で動くプログラムがS3バケットを読み書きするには必ずやAWSの流儀に従って認証と認可のプロセスをPASSしなければならない。AWS CLIをインストールしてconfig操作をすると `~/.aws/credentials` というファイルのができるのだが、その中に プロファイル名とその属性 `aws_access_key_id` と `aws_secret_access_key` が書かれる。アプリケーションプログラムのソースコードの中に プロファイル名 (例えば default)がハードコードされても構わない。しかしプロファイルが持つ２つの属性の値をプログラムのソースコードの中に転記しハードコードすることは絶対に避けるべきだ。プログラムのソースコードがGitレポジトリにpushされた時に認証情報がダダ漏れになってしまうから。わたしが見つけた記事はさすがに `aws_access_key_id` と `aws_secret_access_key`の設定値をソースコードにハードコードしないが、bashシェルで環境変数に設定せよとかjavaコマンド実行時に-Dオプションでプログラムの一部として渡せという方法を採用していた。どちらもイマイチなやり方だ。

## 解決方法

そんななかで[この記事](https://medium.com/@JacobASeverson/s3-maven-repositories-and-gradle-911c25cebeeb)は認証情報を転記するのではなく、Gradleのbuild.gradleが（つまりGroovyスクリプトが） `com.amazonaws.auth.profile.ProfileCredentialsProvider` クラスを呼び出すことにより、 `~/.aws/credential` ファイルの内容を間接的に参照する方法を示してくれていた。`aws_access_key_id` と `aws_secret_access_key`に設定すべき値をアプリケーションのソースコードに転記することなしにS3バケットへアクセスするための認証・認可のプロセスをPASSする方法を明示してくれていた。いいですねえ、これ。この方法を使わせてもらいましょう。
というわけでやって見た。

オリジナルの記事には無かった企みを追加した。自作したJavaコードをAWS Lambdaでラムダ関数として実行したかった。そのためにはJavaコードをjarファイルにしてそれをS3バケットに配置してやる必要があった。どうすればいい？　ーーー　この問題の解決策もここに示すことができた。

# 環境条件

1. AWSアカウントを持っていること
2. PCにAWS CLIをインストール済みで、`~/.aws/credentails`ファイルにプロファイルが登録済みであること
3. 当該プロファイルにはS3バケットを読み書きするのに十分な権限が認可されてあること
4. Gitがインストール済み使えること
5. JDK7+がインストール済みであること

# プロジェクトの構成

`s3-maven-repo`プロジェクトはいくつかのサブプロジェクトから構成されるマルチプロジェクトなGradleプロジェクトである。

- `artifact-repo` : このプロジェクトはMavenレポジトリとなるべきS3バケットを作る。
- `player-api` : このプロジェクトはjarファイルを作り、S3バケット上のMavenレポジトリにjarファイルを格納する。
- `baseball-service` : このプロジェクトはWebサーバアプリを作る。player-apiプロジェクトによって作られMavenレポジトリにpublishされたjarを部品として利用する。

オリジナルのプロジェクトは以上３つのサブプロジェクトを持っていた。

わたしは `s3-maven-repo-example`プロジェクトを少しだけ拡張した。player-apiプロジェクトが開発したJavaライブラリを変更し改修して、AWSでLambda関数として実行できるものに改めた。player-apiをLambdaとして実行するにはjarファイルをS3バケットに配置して、LamdaのランタイムがS3バケットからjarをロードできるようにすることが必要だ。このS3バケットを **artifactsバケット** と呼ぶことにした。わたしはひとつサブプロジェクトを追加した。

- `artifacts-bucket` : このプロジェクトはartifactsバケットとなるべきS3バケットを作る。

# 操作手順

## S3バケットの名前を決める

`gradle.properties`をみよ。

```
repoBucketName=bb4b24b08c-20200319-mvn-repo

artifactsBucketName=bb4b24b08c-20200319-artifacts
```

ここで指定した名前でS3バケットが作られる。

S3バケットの名前はグローバルにユニークでなければならないという制約がある。だからバケット名の先頭に `bb4b24b08c` という暗号っぽい文字列をおいた。これはわたしのAWS
アカウントのIDを種としてmd5ハッシュを生成して得られた32桁の文字列の先頭10桁である。[この記事](https://qiita.com/r-wakatsuki/items/961a0a123055af05fba3)のやり方に倣った。

## MavenレポジトリとなるべきS3バケットを作る

S3バケットを作れ
```
# ./gradlew :artifact-rep:create
```

バケットポリシーを設定せよ
```
# ./gradlew :artifact-repo:setBucketPolicy
```

## player-apiプロジェクトのjarファイルをMavenレポジトリにpublishする

player-apiをビルドしてjarをpublishせよ
```
$ ./gradlew :player-api:publish
```

## player-apiプロジェクトのjarをbaseball-serverプロジェクトを取り込め

```
$ ./gradlew :baseball-service:build
```

## baseball-serverプロジェクトのWebアプリを実行せよ

```
$ java -jar baseball-service/build/libs/baseball-service.jar
```

これでTomcatが立ち上がる。ブラウザで `http://localhost:8080/players` にアクセスせよ。すると下記のようなJSONテキストが応答される。

```
[{"firstName":"Byron","lastName":"Buxton"},{"firstName":"Max","lastName":"Kepler"}]
```

player-apiプロジェクトとbaseball-serviceプロジェクトの成果物がちゃんと動いていることが立証された。


## artifactsバケットを作る

AWS Lambdaで player-api をLambda関数として動かすことを念頭に、S3バケットを作ろう。

```
$ gradle :artifacts-bucket:create
```

artifactsバケットにバケットポリシーを付与する
```
$ gradle :artifacts-bucket:setBucketPolicy
```

## player-apiプロジェクトのjarをartifactsバケットにアップロードする

:player-apiプロジェクトのbuild/libsディレクトリからartifactsバケットへjarをアップロードしよう。

```
$ gradle :player-api:uploadArtifact
```

## 別解　baseball-serverプロジェクトがローカルに取り込み済みのjarをartifactsバケットへ転送する

:baseball-serviceプロジェクトはplayer-apiのjarファイルをMavenレポジトリからダウンロードしてローカルにコピーを持っている。
そのjarをartifactsバケットへ送り込むこともできる。

```
$ gradle :baseball-server:transferArtifact
```

# 結語

以上でartifactsバケットにjarを配置することができた。このjarをAWS Lambda関数として実行することができるはずだが、それはまた別の話。


# 謝意

Jacob A Serversonによるオリジナルの記事 [that post](https://medium.com/@JacobASeverson/s3-maven-repositories-and-gradle-911c25cebeeb)は 
GradleスクリプトがAWS S3バケットを読み書きするために

- [classmethod/Gradle AWS plugin](https://plugins.gradle.org/plugin/jp.classmethod.aws.s3)

を使っていた。わたしもこの便利なGradleプラグインを今後も使わせてもらいます。

このプラグインの作者である都元ダイスケさんが亡くなったという[訃報](https://classmethod.jp/news/farewell-miyamoto/)に接しました。悲しいです。ご冥福をお祈りします。


# 課題

## Classmethod Gradle AWS pluginに対して警告メッセージが出力される

下記のようなメッセージが表示される。

```
~/github/s3-maven-repo-example [develop ≡ +0 ~4 -0 | +0 ~1 -0 !]> gradle :baseball-service:transferArtifact --warning-mode all

> Task :baseball-service:transferArtifact
Property 'bucketName' is not annotated with an input or output annotation. This behaviour has been deprecated and is scheduled to be removed in Gradle 7.0.
Property 'file' is not annotated with an input or output annotation. This behaviour has been deprecated and is scheduled to be removed in Gradle 7.0.
Property 'key' is not annotated with an input or output annotation. This behaviour has been deprecated and is scheduled to be removed in Gradle 7.0.
Property 'kmsKeyId' is not annotated with an input or output annotation. This behaviour has been deprecated and is scheduled to be removed in Gradle 7.0.
Property 'objectMetadata' is not annotated with an input or output annotation. This behaviour has been deprecated and is scheduled to be removed in Gradle 7.0.
Property 'resourceUrl' is not annotated with an input or output annotation. This behaviour has been deprecated and is scheduled to be removed in Gradle 7.0.
Property 'overwrite' is not annotated with an input or output annotation. This behaviour has been deprecated and is scheduled to be removed in Gradle 7.0.

BUILD SUCCESSFUL in 988ms
```

このままではClassmethod Gradle AWS pluginがGradle 7.0で動作しなくなってしまう見込みだ。pluginを開発するプロジェクトのIssueリストにすでに課題が挙げられている。

- https://github.com/classmethod/gradle-aws-plugin/issues/187

もはや都元さんはいないのだから、誰かが代わりにやらなければならない。



