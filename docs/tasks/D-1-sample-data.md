# D-1：示例影视数据

| 项目 | 内容 |
| --- | --- |
| 负责 | 杨同学（bfzzzzzz） |
| 分支 | 第一版：`feature/d-1-sample-data`（已合并）；本次更新：`feature/d-1-pull-script` |
| 截止 | 第一版：11 月 6 日；本次更新：11 月 17 日冻结演示版本之前 |
| 前置 | 本次更新要等 PR #25 合并进 `dev` 之后，再从 `dev` 新建分支（#25 给观看记录加了“一天多条”的日子） |

> **11 月 15 日更新：作品资料改成从 TMDB 整体拉取。** 以前片名、导演、简介这些字段是手写的，海报是另外下载的，几处来源不一致，容易出现“海报和片名不是同一部”“片长和记录对不上”。现在改成一个拉取脚本，按每部作品的 TMDB 编号一次拉齐资料、简介和海报，生成 `library.json`。观看记录、状态、评分这些“用户自己的数据”仍然手写，原样保留。下文已按新做法改写；第一版的要求以本文为准。

## 目标

准备一份约 30 部影视的示例数据，用在课上演示、介绍视频和开发测试里。助教和演示现场都没有真实的观影记录，App 要能一键载入这份数据，看起来像一个真实用户用了一年。

数据分两部分，来源不同：

| 部分 | 字段 | 来源 |
| --- | --- | --- |
| 作品资料 | 片名、原名、年份、题材、导演、主演、简介、片长、集数、海报、`tmdbId`、`imdbId` | 拉取脚本从 TMDB 拉取，**不要手改** |
| 用户数据 | `id`、`type`、状态、评分、标签、观看记录 | 手写，放在 `history.json` |

## 整体接口：一个脚本，一条命令

```
tools/sample-data/
  history.json   手写：每部作品的 id、type、tmdbId，以及状态、评分、标签、观看记录
  pull.py        拉取脚本（语言不限，建议 Python 3，只用标准库）
```

运行一条命令，重新生成全部示例数据：

```
python tools/sample-data/pull.py
```

- **输入：** `history.json`，加上环境变量 `TMDB_API_KEY`（你自己的 TMDB 密钥）。
- **输出：** 覆盖 `app/src/main/assets/sample/library.json`；把海报下载到 `app/src/main/assets/sample/posters/`，并删掉不再用到的旧海报。
- **一致：** 同一部作品的所有资料都来自同一个 TMDB 编号的同一次请求。资料有错就改 `history.json` 里的编号，然后重新运行，不要手改 `library.json`。
- **可重复：** 输入不变时，连续运行两次，结果除了 `pulledAt` 都一样。
- **先检查再写：** 写文件之前先跑完下面“自动检查”的每一条。有一条不通过，就列出全部问题并退出，不写任何文件。

### `history.json` 的格式

```json
{
  "items": [
    {
      "id": "interstellar-2014",
      "type": "movie",
      "tmdbId": "157336",
      "status": "watched",
      "rating": 9.5,
      "tags": ["mind-bending", "rewatched"],
      "records": [
        { "date": "2026-03-14", "episodes": null, "minutes": 169, "rating": 9.0, "note": "First watch, the last act hit hard." }
      ],
      "override": { "genres": ["Sci-Fi", "Drama"] }
    }
  ]
}
```

- `override` 可选，只能覆盖 `genres`、`directors`、`cast` 三个字段，用于 TMDB 资料不合用的个别情况（例如题材映射不出来、剧集没有主创）。每用一次，都要在 PR 里写明原因。
- 第一次做的时候，`status`、`rating`、`tags`、`records` 从现在的 `library.json` 原样搬过来，**不要重新编**。首页测试、演示脚本和热力图的效果都依赖现在这些记录。

## 输出格式（`library.json`）

格式和第一版一样，只多了几个字段：

```json
{
  "version": 1,
  "source": "TMDB",
  "pulledAt": "2026-11-16",
  "items": [
    {
      "id": "interstellar-2014",
      "tmdbId": "157336",
      "imdbId": "tt0816692",
      "title": "Interstellar",
      "originalTitle": "Interstellar",
      "year": 2014,
      "type": "movie",
      "genres": ["Sci-Fi", "Drama"],
      "directors": ["Christopher Nolan"],
      "cast": ["Matthew McConaughey", "Anne Hathaway", "Jessica Chastain", "Michael Caine"],
      "overview": "TMDB 的英文简介原文",
      "runtimeMinutes": 169,
      "totalEpisodes": null,
      "poster": "interstellar-2014.jpg",
      "status": "watched",
      "rating": 9.5,
      "tags": ["mind-bending", "rewatched"],
      "records": [
        { "date": "2026-03-14", "episodes": null, "minutes": 169, "rating": 9.0, "note": "First watch, the last act hit hard." }
      ]
    }
  ]
}
```

App 载入示例数据时会把 `tmdbId` / `imdbId` 存进数据库，以后可以用来去重。`source` 和 `pulledAt` 只是方便查来源，App 不读。

### 每个字段从哪里来

请求时统一加 `language=en-US`。电影用 `/movie/{tmdbId}`，剧集用 `/tv/{tmdbId}`，同一个请求里带上演职员和外部编号（`append_to_response`）。

| 字段 | 电影 | 剧集 |
| --- | --- | --- |
| `tmdbId` | `id` | `id` |
| `imdbId` | `imdb_id` | `external_ids.imdb_id` |
| `title` | `title` | `name` |
| `originalTitle` | `original_title` | `original_name` |
| `year` | `release_date` 的年份 | `first_air_date` 的年份 |
| `genres` | `genres`，按下面的映射表换算 | 同左 |
| `directors` | `credits.crew` 里 `job` 是 `Director` 的人 | `created_by`；为空时用 `override` |
| `cast` | `credits.cast` 按 `order` 取前 4 位 | `aggregate_credits.cast` 按 `order` 取前 4 位 |
| `overview` | `overview` 原文 | 同左 |
| `runtimeMinutes` | `runtime` | `episode_run_time` 的平均值，取整；为空时用 `last_episode_to_air.runtime` |
| `totalEpisodes` | `null` | `number_of_episodes` |
| `poster` | `poster_path` 的 `w500` 尺寸，存成 `<id>.jpg` | 同左 |

### 题材映射表

App 只认这 12 个题材词：Drama, Comedy, Action, Sci-Fi, Animation, Mystery, Crime, Romance, Horror, Documentary, Fantasy, War。

| TMDB 题材 | 换成 |
| --- | --- |
| Action, Adventure, Action & Adventure | Action |
| Science Fiction | Sci-Fi |
| Thriller | Mystery |
| War, War & Politics | War |
| Drama, Comedy, Animation, Mystery, Crime, Romance, Horror, Documentary, Fantasy | 原样保留 |
| Sci-Fi & Fantasy | 映射不出唯一的词，用 `override` 指定 |
| Family, History, Music, Western, TV Movie, Kids, News, Reality, Soap, Talk 等 | 丢掉 |

换算后去掉重复，按 TMDB 的顺序最多保留 3 个。一个都不剩时，必须用 `override` 指定。

## 自动检查（脚本写文件前必须全部通过）

1. 每部作品都有 `tmdbId`；`id` 不重复，`tmdbId` 不重复。
2. 每部作品的片名、年份、简介、海报都拉到了，不能是空的。
3. `genres` 有 1–3 个，而且都在 12 个题材词里；`cast` 最多 4 个。
4. “想看”的作品没有观看记录，评分是 `null`。
5. 电影每条记录的分钟数不超过片长。
6. 剧集每条记录的集数不超过 `totalEpisodes`；分钟数和“集数 × 单集时长”相差不超过 20%。
7. “已看完”的剧集，所有集都在记录里出现过；“在看”的剧集，看过的集数少于 `totalEpisodes`。
8. `library.json` 里的每个海报文件都存在，`posters/` 里没有用不到的文件。

现在的 `library.json` 已经能通过第 3–8 条，所以搬过来的观看记录应该不用改。拉到的新片长或新集数让某条检查不通过时，先在群里说，不要自己改记录。

## 数据要求（验收标准）

1. 一共 30 部：20 部电影、10 部剧集。保留现在的 30 个 `id`，不要换作品：演示脚本和测试是按这些作品写的。
2. 状态分布不变：想看 8 部、在看 5 部、已看完 14 部、弃坑 3 部。The Matrix 仍然是“想看”（演示主线要用它）。
3. 观看记录不变，仍是 63 条，日期在 2025 年 12 月到 2026 年 11 月，每月数量和 `docs/sample-data.md` 一致。首页测试按“11 月 11 条记录、24 小时”验收。
4. 仍然有一段连续 8 天、每天都有记录（2026-01-05 至 01-12），也仍然有一天多条记录的日子（2 月 22 日 4 条），热力图才有深浅。
5. 评分有高有低：至少 2 部 9.5 分以上，至少 2 部低于 5 分，大部分在 6–9 分之间。
6. 至少 3 部作品有 2 次以上观看记录，而且每次评分不同（演示“重看后改分”）。
7. 题材至少覆盖 6 种。
8. 上面“自动检查”8 条全部通过。
9. 抽查 5 部：在 TMDB 网站上按 `tmdbId` 打开，核对片名、年份和海报是同一部作品。
10. 海报是 JPEG，宽 500 像素，`posters/` 目录总大小不超过 3 MB（现在的 PNG 一共 11 MB，会拖慢安装）。
11. JSON 格式正确：在 Android Studio 里打开 `library.json`，右侧没有红色错误标记。

## 版权和密钥

- 资料、简介和海报都来自 TMDB，**必须注明来源**。App 的 About 对话框里已经加了这两句（统筹在 G6 里加的）：“Title details, overviews and posters in the sample library come from TMDB. This product uses the TMDB API but is not endorsed or certified by TMDB.” 介绍视频的片尾字幕也要加上同样两句。
- 只用 TMDB 的资料。不要从豆瓣等其他网站复制，也不要手动从网上另找海报。
- **TMDB 密钥不得出现在仓库里**：脚本只从环境变量 `TMDB_API_KEY` 读取，不要写进代码、配置文件、截图或 PR 描述。提交前用 `git grep` 搜一下自己的密钥，确认搜不到。

## 可以改

- 新建 `tools/sample-data/` 下的文件
- `app/src/main/assets/sample/` 下的文件（只能由脚本生成）
- `docs/sample-data.md`

## 不要改

- 任何 Java 文件、`res/` 下的资源文件、所有 Gradle 文件
- 演示脚本和测试。拉取后片名变了、导致它们对不上时，在群里找统筹。

## 自测

1. 删掉 `posters/` 里的全部文件，运行一次脚本，确认它能从零生成全部数据，而且自动检查全部通过。
2. 再运行一次，用 `git diff` 确认除了 `pulledAt` 没有变化。
3. 在 Android Studio 里打开 `library.json`，确认没有红色错误标记。
4. 编译运行 App，在 Profile 里点 **Load sample data**，看片库、详情页和日记，确认海报和片名对得上。截 3 张图贴进 PR。
5. 把脚本输出的统计（电影和剧集各多少部、每种状态各多少部、每个月几条记录、海报总大小）贴进 PR，并写下抽查了哪 5 部。

## 给 AI 的提示（可以直接复制）

> 请先阅读仓库根目录的 AGENTS.md 和 docs/tasks/D-1-sample-data.md。帮我写 tools/sample-data/pull.py：读 history.json，按任务卡“每个字段从哪里来”和“题材映射表”从 TMDB 拉取资料和海报，生成 library.json；写文件前跑完“自动检查”的 8 条，有不通过就列出全部问题并退出。只用 Python 标准库，密钥只从环境变量 TMDB_API_KEY 读取。再帮我从现在的 library.json 生成 history.json，status、rating、tags、records 原样保留。不要改任务卡“可以改”以外的文件。
