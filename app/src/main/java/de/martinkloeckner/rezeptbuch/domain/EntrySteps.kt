package de.martinkloeckner.rezeptbuch.domain

/** One row of the "An Kamera einstellen" checklist. */
sealed interface EntryItem {
    data class Section(val title: String) : EntryItem

    /** [index] counts only checkable steps (sections excluded), 0-based. */
    data class Step(val index: Int, val label: String, val value: String) : EntryItem
}

/** Value lists in the order of the camera menu (SPEC section 4); used by detail view and entry mode. */
object EntrySteps {

    fun build(r: Recipe): List<EntryItem> {
        val p = r.pp
        val items = mutableListOf<EntryItem>()
        var n = 0
        fun section(t: String) { items += EntryItem.Section(t) }
        fun step(l: String, v: String) { items += EntryItem.Step(n++, l, v) }

        section("Bildprofil")
        step("Schwarzwert", Format.signed(p.blackLevel))
        step("Gamma", Format.text(p.gamma))
        step("Schwarzgamma · Bereich", Format.text(p.blackGamma.range))
        step("Schwarzgamma · Stufe", Format.signed(p.blackGamma.level))
        step("Knie · Modus", Format.text(p.knee.mode))
        if (p.knee.isManual) {
            step("Knie · Punkt", Format.kneePoint(p.knee.point))
            step("Knie · Steigung", Format.signed(p.knee.slope))
        }
        step("Farbmodus", Format.text(p.colorMode))
        step("Sättigung", Format.signed(p.saturation))
        step("Farbphase", Format.signed(p.colorPhase))

        section("Farbtiefe")
        p.depth.entries.forEach { (k, v) -> step("Farbtiefe $k", Format.signed(v)) }

        section("Detail")
        step("Stufe", Format.signed(p.detail.level))
        step("Modus", Format.text(p.detail.mode))
        if (p.detail.isManual) {
            step("V/H-Balance", Format.signed(p.detail.vh))
            step("B/W-Balance", Format.text(p.detail.bw))
            step("Begrenzung", Format.plain(p.detail.limit))
            step("Crispening", Format.plain(p.detail.crisp))
            step("Lichter-Detail", Format.plain(p.detail.hl))
        }

        section("Weißabgleich")
        step("Farbtemperatur", Format.kelvin(r.wb.kelvin))
        step("A-B", Format.ab(r.wb.ab))
        step("G-M", Format.gm(r.wb.gm))
        return items
    }

    fun stepCount(items: List<EntryItem>): Int = items.count { it is EntryItem.Step }
}
