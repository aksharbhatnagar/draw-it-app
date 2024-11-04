package com.example.myapplication.models

data class Document(private val pages: List<Page>) {
    fun getPages() = pages
}
