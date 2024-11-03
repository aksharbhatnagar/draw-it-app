package com.example.myapplication

data class Document(private val pages: List<Page>) {
    fun getPages() = pages
}
