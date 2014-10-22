package io.github.binaryfoo

import java.util.Collections

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.github.binaryfoo.hex.HexDumpElement
import io.github.binaryfoo.tlv.Tag
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import kotlin.platform.platformStatic

/**
 * A rather oddly named class that attempts to order a description of the bits (a decoding) into a hierarchy.
 * Each instance represents a single level in the hierarchy. Leaf nodes have no children.
 *
 * Examples:
 * <ul>
 *     <li>A TLV object with children</li>
 *     <li>A primitive value interpreted as a bit string, like TVR, where the children are the "on" bits.</li>
 * </ul>
 */
JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public data class DecodedData(
        val tag: Tag?,
        val rawData: String, // Not a great name. Used as a label for the decoded data.
        val fullDecodedData: String,
        val startIndex: Int, // in bytes
        val endIndex: Int, // in bytes
        kids: List<DecodedData> = listOf()) {
    public var notes: String? = null
    public var hexDump: List<HexDumpElement>? = null
    public val children: List<DecodedData> = kids
    get() {
        if ($children.isEmpty() && notes != null) {
            return listOf(primitive("", notes!!, 0, 0))
        }
        return $children
    }

    private fun trim(decodedData: String): String {
        return if (decodedData.length() >= 60) decodedData.substring(0, 56) + "..." + StringUtils.right(decodedData, 4) else decodedData
    }

    /**
     * For an element with children this is usually the hex value (ie the real raw data).
     * For an element without children this is the decoded value: ascii, numeric, enumerated or even just hex.
     */
    public fun getDecodedData(): String {
        return if (isComposite()) trim(fullDecodedData) else fullDecodedData
    }

    public fun getChild(index: Int): DecodedData {
        return children.get(index)
    }

    public fun isComposite(): Boolean {
        return !children.isEmpty()
    }

    override fun toString(): String {
        var s = "raw=[${rawData}] decoded=[${fullDecodedData}] indexes=[${startIndex},${endIndex}]"
        if (isComposite()) {
            for (d in children) {
                s += "\n" + d
            }
        }
        return s
    }

    class object {

        platformStatic public fun primitive(rawData: String, decodedData: String, startIndex: Int, endIndex: Int): DecodedData {
            return DecodedData(null, rawData, decodedData, startIndex, endIndex, listOf<DecodedData>())
        }

        platformStatic public fun constructed(rawData: String, decodedData: String, startIndex: Int, endIndex: Int, children: List<DecodedData>): DecodedData {
            return DecodedData(null, rawData, decodedData, startIndex, endIndex, children)
        }

        platformStatic public fun fromTlv(tag: Tag, rawData: String, decodedData: String, startIndex: Int, endIndex: Int, children: List<DecodedData>): DecodedData {
            return DecodedData(tag, rawData, decodedData, startIndex, endIndex, children)
        }

        platformStatic public fun findForTag(tag: Tag, decoded: List<DecodedData>): DecodedData? {
            decoded.forEach {
                if (it.tag == tag) {
                    return it
                }
                val match = it.children.findForTag(tag)
                if (match != null) {
                    return match
                }
            }
            return null
        }

        platformStatic public fun findForValue(value: String, decoded: List<DecodedData>): DecodedData? {
            decoded.forEach {
                if (it.fullDecodedData == value) {
                    return it
                }
                val match = it.children.findForValue(value)
                if (match != null) {
                    return match
                }
            }
            return null
        }

    }

}

public fun List<DecodedData>.findForTag(tag: Tag): DecodedData? {
    return DecodedData.findForTag(tag, this)
}

public fun List<DecodedData>.findForValue(value: String): DecodedData? {
    return DecodedData.findForValue(value, this)
}