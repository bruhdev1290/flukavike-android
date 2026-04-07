package com.fluxer.client.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fluxer.client.data.model.Message
import com.fluxer.client.data.remote.FluxerApiService
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

private const val STARTING_PAGE_INDEX = 0

class MessagePagingSource(
    private val apiService: FluxerApiService,
    private val channelId: String
) : PagingSource<String, Message>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Message> {
        val before = params.key
        return try {
            Timber.d("Loading messages for channel $channelId, before=$before, limit=${params.loadSize}")
            
            val response = apiService.getMessages(channelId = channelId, before = before, limit = params.loadSize)
            
            if (!response.isSuccessful) {
                Timber.e("Failed to load messages: ${response.code()} - ${response.errorBody()?.string()}")
                return LoadResult.Error(HttpException(response))
            }
            
            val messages = response.body() ?: emptyList()
            Timber.d("Loaded ${messages.size} messages for channel $channelId")
            
            // Log first few message IDs for debugging
            if (messages.isNotEmpty()) {
                Timber.d("First message: ${messages.first().id}, content: ${messages.first().content.take(50)}")
            }

            LoadResult.Page(
                data = messages,
                prevKey = null, // Only paging backward
                nextKey = messages.lastOrNull()?.id
            )
        } catch (e: IOException) {
            Timber.e(e, "Network error loading messages for channel $channelId")
            LoadResult.Error(e)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error loading messages for channel $channelId")
            LoadResult.Error(e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error loading messages for channel $channelId")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Message>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
