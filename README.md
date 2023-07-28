# MinecraftAdvancementSync
マインクラフトの進捗を全員で同期（共有）させるプラグイン

<img width="492" alt="2023-07-28 (4)" src="https://github.com/comorichico/MinecraftAdvancementSync/assets/96755854/9b34bd8b-3451-4769-ad28-b92c47a631ba">

使い方

pluginsのフォルダに入れるだけ

・WEBサーバーで進捗を見れるようにする場合

pluginsにMinecraftAdvancementSyncというフォルダが生成されてその中に.envファイルが生成されているはずです

serverにFTPサーバーの接続に使うドメイン、usernameに接続に使うユーザー名、passwordにFTPのパスワードを設定してください

リモートのfolderはデフォルトで/masになっていますが好きに変えてもいいです、ほかのデータが上書きされないように注意してください。

.envの設定が間違っていなければゲーム内で「/mas」とコマンドを打てばサーバーにアップロードされるはずです
