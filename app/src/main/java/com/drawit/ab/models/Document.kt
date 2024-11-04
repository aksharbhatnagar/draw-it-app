package com.drawit.ab.models

data class Document(private val pages: List<Page>) {
    fun getPages() = pages
}
