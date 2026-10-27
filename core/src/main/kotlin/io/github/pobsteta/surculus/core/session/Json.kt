/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

/**
 * JSON minimal, en Kotlin pur, pour la `valeur` des lignes du journal (`docs/formats/journal-valeurs-v1.md`).
 * Objets, chaînes, nombres, booléens et `null` ; pas de tableaux (le format n'en porte pas). Les clés sont écrites
 * dans l'ordre d'insertion, si bien que la même saisie donne toujours le même texte (une ligne reçue deux fois
 * « à l'identique » l'est caractère pour caractère, spec § 5).
 */
internal object Json {
    fun ecrire(objet: Map<String, Any?>): String = StringBuilder().also { ecrireObjet(it, objet) }.toString()

    private fun ecrireObjet(sb: StringBuilder, objet: Map<String, Any?>) {
        sb.append('{')
        objet.entries.forEachIndexed { i, (cle, v) ->
            if (i > 0) sb.append(',')
            ecrireTexte(sb, cle)
            sb.append(':')
            ecrireValeur(sb, v)
        }
        sb.append('}')
    }

    private fun ecrireValeur(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is String -> ecrireTexte(sb, v)
            is Boolean -> sb.append(v)
            is Int, is Long -> sb.append(v)
            is Double -> {
                require(v.isFinite()) { "nombre non fini dans une valeur du journal : $v" }
                sb.append(v)
            }
            is Map<*, *> -> ecrireObjet(sb, v.entries.associate { (k, x) -> k as String to x })
            else -> throw IllegalArgumentException("type non pris en charge : ${v::class}")
        }
    }

    private fun ecrireTexte(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' -> sb.append("\\u%04x".format(c.code))
                else -> sb.append(c)
            }
        }
        sb.append('"')
    }

    /** Lit un objet JSON ; `null` si le texte n'en est pas un (valeur illisible : la ligne est ignorée, jamais devinée). */
    fun lire(texte: String?): Map<String, Any?>? {
        if (texte == null) return null
        return try {
            val l = Lecteur(texte)
            val o = l.objet()
            l.fin()
            o
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private class Lecteur(private val s: String) {
        private var i = 0

        private fun blancs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun attendre(c: Char) {
            blancs()
            require(i < s.length && s[i] == c) { "« $c » attendu en $i" }
            i++
        }

        fun fin() {
            blancs()
            require(i == s.length) { "texte après l'objet" }
        }

        fun objet(): Map<String, Any?> {
            attendre('{')
            val m = LinkedHashMap<String, Any?>()
            blancs()
            if (i < s.length && s[i] == '}') {
                i++
                return m
            }
            while (true) {
                blancs()
                val cle = texte()
                attendre(':')
                m[cle] = valeur()
                blancs()
                require(i < s.length) { "objet non fermé" }
                if (s[i] == ',') {
                    i++
                    continue
                }
                require(s[i] == '}') { "« } » attendu en $i" }
                i++
                return m
            }
        }

        private fun valeur(): Any? {
            blancs()
            require(i < s.length) { "valeur attendue" }
            return when (val c = s[i]) {
                '{' -> objet()
                '"' -> texte()
                't' -> mot("true", true)
                'f' -> mot("false", false)
                'n' -> mot("null", null)
                else -> {
                    require(c == '-' || c.isDigit()) { "valeur inattendue en $i" }
                    nombre()
                }
            }
        }

        private fun mot(m: String, v: Any?): Any? {
            require(s.startsWith(m, i)) { "« $m » attendu en $i" }
            i += m.length
            return v
        }

        private fun nombre(): Any {
            val debut = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            val t = s.substring(debut, i)
            return try {
                if (t.any { it in ".eE" }) t.toDouble() else t.toLong()
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("nombre illisible : $t")
            }
        }

        private fun texte(): String {
            require(i < s.length && s[i] == '"') { "chaîne attendue en $i" }
            i++
            val sb = StringBuilder()
            while (true) {
                require(i < s.length) { "chaîne non fermée" }
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        require(i < s.length) { "échappement coupé" }
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                require(i + 4 <= s.length) { "\\u coupé" }
                                sb.append(s.substring(i, i + 4).toInt(16).toChar())
                                i += 4
                            }
                            else -> throw IllegalArgumentException("échappement inconnu \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }
    }
}
