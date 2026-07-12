package com.networth.tracker.data.cas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CasTextParserTest {

    @Test
    fun parsesDetailedCasValuationBlocks() {
        val text = """
            HDFC Mutual Fund
            Folio No: 12345678 / 0 PAN: ABCDE1234F KYC: OK
            D110-HDFC Flexi Cap Fund - Direct Plan - Growth ISIN: INF179KA1RT1
            Registrar : CAMS
            Opening Unit Balance: 100.000
            Closing Unit Balance: 110.500
            NAV on 31-Mar-2026 : INR 850.25
            Valuation on 31-Mar-2026 : INR 93902.63
            Total Cost Value: 75000.00
            
            Folio No: 99887766 / 0
            P1234-Parag Parikh Flexi Cap Fund - Direct Plan - Growth
            Closing Unit Balance: 50.000
            NAV on 31-Mar-2026 : INR 80.00
            Market Value on 31-Mar-2026 : INR 4000.00
            Total Cost Value: 3500.00
        """.trimIndent()

        val result = CasTextParser.parse(text)
        assertEquals(2, result.holdings.size)
        assertEquals(75000.0, result.holdings[0].investedAmount, 0.01)
        assertEquals(93902.63, result.holdings[0].currentAmount, 0.01)
        assertTrue(result.holdings[0].returnPercent > 0)
        assertEquals(3500.0, result.holdings[1].investedAmount, 0.01)
        assertEquals(4000.0, result.holdings[1].currentAmount, 0.01)
    }

    @Test
    fun parsesSummaryCasRows() {
        val text = """
            Folio No. ISIN Scheme Name Cost Value Unit Balance NAV Date NAV Market Value
            12345678 INF179KA1RT1 HDFC Flexi Cap Fund - Direct Growth 50000.00 100.0000 31-Mar-2026 600.00 60000.00
            as on 31-Mar-2026
        """.trimIndent()

        val result = CasTextParser.parse(text)
        assertEquals(1, result.holdings.size)
        assertEquals(50000.0, result.holdings[0].investedAmount, 0.01)
        assertEquals(60000.0, result.holdings[0].currentAmount, 0.01)
        assertEquals(20.0, result.holdings[0].returnPercent, 0.01)
        assertEquals("31-Mar-2026", result.statementAsOf)
    }

    @Test
    fun parsesJumbledSummaryAcrossLines() {
        val text = """
            Consolidated Account Statement
            Folio No. ISIN Scheme Name Cost Value Closing Unit Balance NAV Date NAV Market Value Registrar
            91014991234
            INF209KB1YQ1
            Nippon India Small Cap Fund - Direct Plan Growth
            125000.50
            842.123
            31-Mar-2026
            168.45
            141850.75
            CAMS
            9988776612 / 0
            INF179KA1RT1
            HDFC Flexi Cap Fund - Direct Plan - Growth
            50000.00
            100.0000
            31-Mar-2026
            600.00
            60000.00
            CAMS
            Grand Total
        """.trimIndent()

        val result = CasTextParser.parse(text)
        assertEquals(2, result.holdings.size)
        assertEquals("INF209KB1YQ1", result.holdings[0].isin)
        assertEquals(125000.50, result.holdings[0].investedAmount, 0.01)
        assertEquals(141850.75, result.holdings[0].currentAmount, 0.01)
        assertTrue(result.holdings[0].schemeName.contains("Nippon", ignoreCase = true))
        assertEquals(50000.0, result.holdings[1].investedAmount, 0.01)
        assertEquals(60000.0, result.holdings[1].currentAmount, 0.01)
    }

    @Test
    fun parsesSummaryWithGluedColumns() {
        val text = """
            12345678/0 INF179KA1RT1 HDFC Flexi Cap Fund Direct Growth 50000.00 100.0000 31-Mar-2026 600.00 60000.00 CAMS
        """.trimIndent()

        val result = CasTextParser.parse(text)
        assertEquals(1, result.holdings.size)
        assertEquals(50000.0, result.holdings[0].investedAmount, 0.01)
        assertEquals(60000.0, result.holdings[0].currentAmount, 0.01)
    }

    @Test
    fun mergesPartialStrategiesToRecoverAllRows() {
        val text = """
            Folio No. ISIN Scheme Name Cost Value Unit Balance NAV Date NAV Market Value
            11111111 INF179KA1RT1 Clean Fund One 1000.00 10.0000 31-Mar-2026 110.00 1100.00
            22222222 INF209KB1YQ1 Clean Fund Two 2000.00 20.0000 31-Mar-2026 120.00 2400.00
            33333333
            INF846K01AAA
            Wrapped Fund Three Direct Growth
            3000.00
            30.000
            31-Mar-2026
            130.00
            3900.00
            CAMS
            44444444/0 INF090I01234 Glued Fund Four Growth 4000.00 40.00 31-Mar-2026 140.00 5600.00 KFINTECH
        """.trimIndent()

        val result = CasTextParser.parse(text)
        assertEquals(4, result.holdings.size)
        assertEquals(1000.0 + 2000.0 + 3000.0 + 4000.0, result.totalInvested, 0.01)
        assertEquals(1100.0 + 2400.0 + 3900.0 + 5600.0, result.totalCurrent, 0.01)
    }

    @Test
    fun keepsSeparateFoliosWithSameIsin() {
        val text = """
            Folio No. ISIN Scheme Name Cost Value Unit Balance NAV Date NAV Market Value Registrar
            1040369504 INF209K01UN8 Aditya Birla ELSS 35000.000 772.307 10-Jul-2026 69.77 53883.86 CAMS
            1045049512 INF209K01UN8 Aditya Birla ELSS 3000.000 60.686 10-Jul-2026 69.77 4234.06 CAMS
            91098369846/0 INF846K01EW2 Axis ELSS 36500.000 491.551 10-Jul-2026 109.7969 53970.78 KFINTECH
        """.trimIndent()

        val result = CasTextParser.parse(text)
        assertEquals(3, result.holdings.size)
        assertEquals(1, result.holdings.count { it.folio.replace(" ", "") == "1040369504" })
        assertEquals(1, result.holdings.count { it.folio.replace(" ", "") == "1045049512" })
        assertEquals("INF209K01UN8", result.holdings.first { it.folio.replace(" ", "") == "1040369504" }.isin)
        assertEquals("INF209K01UN8", result.holdings.first { it.folio.replace(" ", "") == "1045049512" }.isin)
    }

    @Test
    fun rejectsPincodeButAcceptsRealFolios() {
        assertFalse(CasSummaryTableParser.isValidFolio("273013"))
        assertFalse(CasSummaryTableParser.isValidFolio("110001"))
        assertTrue(CasSummaryTableParser.isValidFolio("2954471"))
        assertTrue(CasSummaryTableParser.isValidFolio("15304620"))
        assertTrue(CasSummaryTableParser.isValidFolio("15423450/1"))
        assertTrue(CasSummaryTableParser.isValidFolio("5936830/08"))
        assertTrue(CasSummaryTableParser.isValidFolio("910900085942"))
        assertTrue(CasSummaryTableParser.isValidFolio("2028681/15"))
    }

    @Test
    fun parsesRealCamsSummaryMarketFirstLayout() {
        // Exact column-jumble shape produced by CAMS Summary PDF text extract.
        val text = """
            Consolidated Account Summary
            As on 12-Jul-2026
            GORAKHPUR - 273013
            Market ValueFolio No.
            (INR)
            Scheme Name Unit Balance
            NAV Date NAV Registrar
            (INR)
            ISIN Cost Value
            (INR)
            1040369504 53,883.86B02GZ - Aditya Birla Sun Life ELSS Tax
            Saver Fund- (ELSS U/S 80C of IT ACT) -
            Growth-Direct Plan (Non-Demat)
            772.307 10-Jul-2026 69.77 CAMSINF209K01UN8 35,000.000
            1045049512 4,234.06B02GZ - Aditya Birla Sun Life ELSS Tax
            Saver Fund- (ELSS U/S 80C of IT ACT) -
            Growth-Direct Plan (Non-Demat)
            60.686 10-Jul-2026 69.77 CAMSINF209K01UN8 3,000.000
            91098369846/0 53,970.78128TSDGG - Axis ELSS Tax Saver Fund -
            Direct Growth (Non Demat )
            491.551 10-Jul-2026 109.7969 KFINTECHINF846K01EW2 36,500.000
            910173645936/0 4,073.25128TSDGG - Axis ELSS Tax Saver Fund -
            Direct Growth (Non Demat )
            37.098 10-Jul-2026 109.7969 KFINTECHINF846K01EW2 3,000.000
            3652059/70 1,629.35GD223 - Bandhan ELSS Tax saver Fund-
            Direct Plan-Growth (Non-Demat)
            9.077 10-Jul-2026 179.503 CAMSINF194K01Y29 1,000.000
            17745272266/0 1,591.83101ETDGG - Canara Robeco ELSS Tax Saver
            Fund - Direct Growth (Non Demat )
            7.871 10-Jul-2026 202.24 KFINTECHINF760K01EL8 1,000.000
            7987461/12 1,768.85D739 - DSP ELSS Tax Saver Fund - Direct
            Plan - Growth (Non-Demat)
            11.288 10-Jul-2026 156.702 CAMSINF740K01OK1 1,000.000
            34326873/13 46,651.17HNEWDG - HDFC NIFTY50 Equal Weight
            Index Fund Direct Growth (Demat)
            2,509.922 10-Jul-2026 18.5867 CAMSINF179KC1BM8 44,000.000
            23386003/54 30,695.80P8000 - ICICI Prudential ELSS Tax Saver
            Fund - Direct Plan - Growth (Non-Demat)
            29.381 10-Jul-2026 1,044.75 CAMSINF109K01Y31 20,500.000
            38150635/44 101,786.63P8042 - ICICI Prudential Large Cap Fund
            (erstwhile Bluechip Fund) - Direct Plan -
            Growth (formerly ICICI Prudential Focused
            Bluechip Equity Fund) (Demat)
            845.053 10-Jul-2026 120.45 CAMSINF109K016L0 101,000.000
            3106627354/0 64,312.52120TPD1G - Invesco India ELSS Tax Saver
            Fund - Direct Plan Growth (Non Demat )
            432.324 10-Jul-2026 148.76 KFINTECHINF205K01NT8 40,000.000
            31017681671/0 4,185.36120TPD1G - Invesco India ELSS Tax Saver
            Fund - Direct Plan Growth (Non Demat )
            28.135 10-Jul-2026 148.76 KFINTECHINF205K01NT8 3,000.000
            11045602 28,413.93K144D - Kotak ELSS Tax Saver Fund -
            Direct Plan - Growth (Non-Demat)
            205.743 10-Jul-2026 138.104 CAMSINF174K01LI3 19,000.000
            77760506306/0 1,700.89117TSD1G - Mirae Asset ELSS Tax Saver
            Fund (formerly Mirae Asset Tax Saver
            Fund ) - Direct Plan (Non Demat )
            29.304 10-Jul-2026 58.043 KFINTECHINF769K01DM9 1,000.000
            31044317 32,620.78LD018G - SBI ELSS Tax Saver Fund - Direct
            Plan - Growth (Non-Demat)
            68.184 10-Jul-2026 478.4228 CAMSINF200K01UM9 19,000.000
            8128002/48 31,900.32TTSFGZ - Tata ELSS Fund Direct Plan
            Growth (Non-Demat)
            591.064 10-Jul-2026 53.9710 CAMSINF277K01I86 20,500.000
            Total 463,419.38348,500.00
        """.trimIndent()

        val result = CasTextParser.parse(text)
        assertEquals(16, result.holdings.size)
        assertEquals(348500.0, result.totalInvested, 0.01)
        assertEquals(463419.38, result.totalCurrent, 0.01)
        assertEquals("12-Jul-2026", result.statementAsOf)

        // Same ISIN on two folios must stay separate.
        assertEquals(2, result.holdings.count { it.isin == "INF209K01UN8" })
        assertEquals(2, result.holdings.count { it.isin == "INF846K01EW2" })
        assertEquals(2, result.holdings.count { it.isin == "INF205K01NT8" })

        val absL = result.holdings.first { it.folio == "1040369504" }
        assertEquals(35000.0, absL.investedAmount, 0.01)
        assertEquals(53883.86, absL.currentAmount, 0.01)
        assertEquals(772.307, absL.units, 0.001)
        assertEquals(69.77, absL.nav, 0.001)
        assertTrue(absL.schemeName.contains("Aditya Birla", ignoreCase = true))

        val largeCap = result.holdings.first { it.folio == "38150635/44" }
        assertEquals(101000.0, largeCap.investedAmount, 0.01)
        assertEquals(101786.63, largeCap.currentAmount, 0.01)
        assertEquals("INF109K016L0", largeCap.isin)

        // Address PIN must never become a holding.
        assertTrue(result.holdings.none { it.folio == "273013" })
    }
}
