package com.mytv.live.data

/**
 * 频道分组（决定首页里的分区标题）。
 */
enum class ChannelGroup(val title: String) {
    CCTV("央视频道"),
    LOCAL("地方频道"),
}

/**
 * 播放器类型 —— 决定自动全屏时该点哪个全屏按钮选择器。
 *
 * - [CCTV] 央视官网 tv.cctv.com 的网页播放器：`#player_pagefullscreen_yes_player`
 * - [YANGSHIPIN] 央视频 yangshipin.cn 的播放器：`.videoFull`
 */
enum class PlayerKind { CCTV, YANGSHIPIN }

data class Channel(
    val id: Int,
    val name: String,
    val url: String,
    val kind: PlayerKind,
    val group: ChannelGroup,
)

/**
 * 频道清单。URL 与名称均移植自 ../CCTV_Viewer（已验证可用），
 * 前 20 个为央视官网频道，后 21 个为央视频地方卫视。
 */
object Channels {

    private val cctv: List<Channel> = listOf(
        "CCTV-1 综合"          to "https://tv.cctv.com/live/cctv1/",
        "CCTV-2 财经"          to "https://tv.cctv.com/live/cctv2/",
        "CCTV-3 综艺"          to "https://tv.cctv.com/live/cctv3/",
        "CCTV-4 中文国际（亚）" to "https://tv.cctv.com/live/cctv4/",
        "CCTV-5 体育"          to "https://tv.cctv.com/live/cctv5/",
        "CCTV-6 电影"          to "https://tv.cctv.com/live/cctv6/",
        "CCTV-7 国防军事"      to "https://tv.cctv.com/live/cctv7/",
        "CCTV-8 电视剧"        to "https://tv.cctv.com/live/cctv8/",
        "CCTV-9 纪录"          to "https://tv.cctv.com/live/cctvjilu",
        "CCTV-10 科教"         to "https://tv.cctv.com/live/cctv10/",
        "CCTV-11 戏曲"         to "https://tv.cctv.com/live/cctv11/",
        "CCTV-12 社会与法"      to "https://tv.cctv.com/live/cctv12/",
        "CCTV-13 新闻"         to "https://tv.cctv.com/live/cctv13/",
        "CCTV-14 少儿"         to "https://tv.cctv.com/live/cctvchild",
        "CCTV-15 音乐"         to "https://tv.cctv.com/live/cctv15/",
        "CCTV-16 奥林匹克"      to "https://tv.cctv.com/live/cctv16/",
        "CCTV-17 农业农村"      to "https://tv.cctv.com/live/cctv17/",
        "CCTV-5+ 体育赛事"      to "https://tv.cctv.com/live/cctv5plus/",
        "CCTV-4 中文国际（欧）" to "https://tv.cctv.com/live/cctveurope",
        "CCTV-4 中文国际（美）" to "https://tv.cctv.com/live/cctvamerica/",
    ).mapIndexed { i, (name, url) ->
        Channel(id = i, name = name, url = url, kind = PlayerKind.CCTV, group = ChannelGroup.CCTV)
    }

    private val local: List<Channel> = listOf(
        "北京卫视"   to "600002309",
        "江苏卫视"   to "600002521",
        "东方卫视"   to "600002483",
        "浙江卫视"   to "600002520",
        "湖南卫视"   to "600002475",
        "湖北卫视"   to "600002508",
        "广东卫视"   to "600002485",
        "广西卫视"   to "600002509",
        "黑龙江卫视" to "600002498",
        "海南卫视"   to "600002506",
        "重庆卫视"   to "600002531",
        "深圳卫视"   to "600002481",
        "四川卫视"   to "600002516",
        "河南卫视"   to "600002525",
        "福建东南卫视" to "600002484",
        "贵州卫视"   to "600002490",
        "江西卫视"   to "600002503",
        "辽宁卫视"   to "600002505",
        "安徽卫视"   to "600002532",
        "河北卫视"   to "600002493",
        "山东卫视"   to "600002513",
    ).mapIndexed { i, (name, pid) ->
        Channel(
            id = cctv.size + i,
            name = name,
            url = "https://www.yangshipin.cn/tv/home?pid=$pid",
            kind = PlayerKind.YANGSHIPIN,
            group = ChannelGroup.LOCAL,
        )
    }

    /** 全部频道，id 即为下标，顺序与 ../CCTV_Viewer 一致。 */
    val all: List<Channel> = cctv + local

    fun byId(id: Int): Channel? = all.getOrNull(id)

    /** 上一个频道（到头循环）。 */
    fun prev(id: Int): Channel = all[(id - 1 + all.size) % all.size]

    /** 下一个频道（到尾循环）。 */
    fun next(id: Int): Channel = all[(id + 1) % all.size]

    val groups: List<Pair<ChannelGroup, List<Channel>>> = listOf(
        ChannelGroup.CCTV to cctv,
        ChannelGroup.LOCAL to local,
    )
}
