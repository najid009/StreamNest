package com.ngi.streamnest.data

/** Backend কনফিগার না থাকলে বা লোড ব্যর্থ হলে দেখানোর নমুনা ডেটা */
object SampleData {

    private const val SPORTS_STREAM = "https://tvsen6.aynaott.com/6xyZ3N4oHv2KBJdB6W4p/index.m3u8"

    val channels = listOf(
        Channel(
            id = "sample-ch-1",
            name = "Sports Channel 1",
            streamUrl = SPORTS_STREAM,
            category = "sports",
            sortOrder = 1,
        ),
    )

    val featured = listOf(
        FeaturedItem(
            id = "sample-f-1",
            title = "আপনার বিজ্ঞাপন এখানে দেখাবে",
            imageUrl = "https://picsum.photos/seed/streamnest-ad/800/450",
            actionType = "url",
            actionValue = "https://example.com",
            sortOrder = 1,
        ),
        FeaturedItem(
            id = "sample-f-2",
            title = "Sports Channel 1 এখন লাইভ",
            imageUrl = "https://picsum.photos/seed/streamnest-live/800/450",
            actionType = "channel",
            actionValue = "sample-ch-1",
            sortOrder = 2,
        ),
    )

    val matches = listOf(
        MatchItem(
            id = "sample-m-1",
            sport = "football",
            title = "Team A বনাম Team B",
            league = "নমুনা লিগ",
            teamA = "Team A",
            teamB = "Team B",
            startTime = null,
            status = "live",
            score = "1 - 0",
            channelId = "sample-ch-1",
        ),
        MatchItem(
            id = "sample-m-2",
            sport = "cricket",
            title = "Team X বনাম Team Y",
            league = "নমুনা সিরিজ",
            teamA = "Team X",
            teamB = "Team Y",
            startTime = "2026-10-12T14:00:00+06:00",
            status = "upcoming",
            score = null,
            channelId = null,
        ),
    )
}
