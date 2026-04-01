package com.fluxer.client.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fluxer.client.data.model.Message
import com.fluxer.client.data.remote.FluxerApiService
import retrofit2.HttpException
import java.io.IOException

private const val STARTING_PAGE_INDEX = 0

class MessagePagingSource(
    private val apiService: FluxerApiService,
    private val channelId: String
) : PagingSource<String, Message>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Message> {
        val before = params.key
        return try {
            val response = apiService.getMessages(channelId = channelId, before = before, limit = params.loadSize)
            val messages = response.body() ?: emptyList()

            LoadResult.Page(
                data = messages,
                prevKey = null, // Only paging backward
                nextKey = messages.lastOrNull()?.id
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Message>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
