package com.ngi.streamnest.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class HomeData(
    val featured: List<FeaturedItem>,
    val channels: List<Channel>,
    val matches: List<MatchItem>,
    val isSample: Boolean,
    val error: String? = null,
)

fun HomeData.channelById(id: String?): Channel? =
    if (id == null) null else channels.firstOrNull { it.id == id }

object ContentRepository {

    suspend fun load(): HomeData {
        if (!SupabaseApi.isConfigured) {
            return sample("Backend কনফিগার করা নেই, নমুনা ডেটা দেখানো হচ্ছে")
        }
        return try {
            coroutineScope {
                val channels = async { SupabaseApi.channels() }
                val featured = async { SupabaseApi.featured() }
                val matches = async { SupabaseApi.matches() }
                HomeData(
                    featured = featured.await(),
                    channels = channels.await(),
                    matches = matches.await(),
                    isSample = false,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sample("ডেটা লোড হয়নি: ${e.message}")
        }
    }

    private fun sample(error: String) = HomeData(
        featured = SampleData.featured,
        channels = SampleData.channels,
        matches = SampleData.matches,
        isSample = true,
        error = error,
    )
}
