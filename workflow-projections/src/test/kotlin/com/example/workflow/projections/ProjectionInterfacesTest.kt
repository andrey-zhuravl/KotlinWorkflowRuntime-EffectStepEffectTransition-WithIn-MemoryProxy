package com.example.workflow.projections

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectionInterfacesTest {
    @Test
    fun `offset remembers sequence`() {
        val offset = Offset(42)
        assertEquals(42, offset.journalSeq)
    }
}
