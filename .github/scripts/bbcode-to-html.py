#!/usr/bin/env python3
"""Convert a MyPet changelog from XenForo BBCode to the HTML Voxel.Shop stores.

BuiltByBit (XenForo) takes the changelog as BBCode. Voxel.Shop — Polymart rebranded —
stores an update's `update_description` as HTML and does not parse BBCode, so posting the
raw changelog there renders literal "[B]…[/B]" text. Everything this script emits was
read back off the 4.0.0 update (posted by hand through the web UI, so it is Voxel's own
rendering of the same source file): <p>, <strong>, <em>, <code>, <a>, <h2>, <h3>, <ul>,
<li>, with &, < and > escaped as entities and bare URLs turned into links.

Deliberately strict: an unrecognised BBCode tag is an error, not something to pass through
or silently drop. The release workflow runs this before it tags, uploads or posts anything,
so a changelog using a tag this script has never seen fails the run while it is still free
to re-dispatch — rather than publishing "[SIZE=4]" to the store as visible text.

Usage: bbcode-to-html.py <changelog.bbcode>   # HTML on stdout
"""
import html
import re
import sys

HEADING = re.compile(r"\[HEADING=([1-6])\](.*?)\[/HEADING\]", re.DOTALL)
URL_TAG = re.compile(r"\[URL=([^\]]+)\](.*?)\[/URL\]", re.DOTALL)
ICODE = re.compile(r"\[ICODE\](.*?)\[/ICODE\]", re.DOTALL)
BOLD = re.compile(r"\[B\](.*?)\[/B\]", re.DOTALL)
ITALIC = re.compile(r"\[I\](.*?)\[/I\]", re.DOTALL)
BARE_URL = re.compile(r"https?://[^\s<>\"]+")
TRAILING_PUNCTUATION = ".,;:!?)"
# Anything that still looks like a tag after conversion is a tag we don't handle.
LEFTOVER = re.compile(r"\[/?[A-Za-z*][^\]]*\]")
BLOCK_START = ("[LIST", "[HEADING")


class Unsupported(Exception):
    pass


def inline(text, holes, escape=True, in_link=False):
    """Convert the inline tags in one already-joined block of text.

    Recurses into link labels (`escape=False`, since the text is escaped once on the way
    in) because BBCode nests there: the 4.0.0 changelog writes
    [URL='…'][B]Migrating from MyPet 3[/B][/URL], and parking the finished <a> before the
    bold pass runs would leave a literal "[B]" inside it.
    """
    out = html.escape(text, quote=False) if escape else text

    def stash(replacement):
        """Park finished HTML so later passes can't reprocess its contents."""
        holes.append(replacement)
        return f"\x00{len(holes) - 1}\x00"

    def render_url(match):
        # XenForo writes both [URL=x] and [URL='x']; the quotes are delimiters, not href.
        href = match.group(1).strip().strip("\"'").replace('"', "&quot;")
        label = inline(match.group(2), holes, escape=False, in_link=True)
        return stash(f'<a href="{href}">{label}</a>')

    def linkify(match):
        # Trailing sentence punctuation belongs to the prose, not the URL.
        url = match.group(0).rstrip(TRAILING_PUNCTUATION)
        tail = match.group(0)[len(url):]
        return stash(f'<a href="{url}">{url}</a>') + tail

    # Links and code first: a URL inside [ICODE] is sample text, not a link, and a
    # [URL=] href must not be linkified a second time.
    out = URL_TAG.sub(render_url, out)
    out = ICODE.sub(lambda m: stash(f"<code>{m.group(1)}</code>"), out)
    if not in_link:
        # Nested anchors aren't valid HTML, so a bare URL inside a link label stays text.
        out = BARE_URL.sub(linkify, out)
    out = BOLD.sub(lambda m: f"<strong>{m.group(1)}</strong>", out)
    out = ITALIC.sub(lambda m: f"<em>{m.group(1)}</em>", out)
    return out


def convert(source):
    if "\x00" in source:
        raise Unsupported("changelog contains a NUL byte")

    holes = []
    lines = source.split("\n")
    blocks = []
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue

        if line.startswith("[LIST"):
            items = []
            i += 1
            while i < len(lines) and not lines[i].strip().startswith("[/LIST]"):
                item = lines[i].strip()
                if item.startswith("[*]"):
                    items.append(item[3:].strip())
                elif item:
                    if not items:
                        raise Unsupported(f"line {i + 1}: text inside [LIST] before any [*]")
                    items[-1] += " " + item  # a wrapped list item
                i += 1
            if i >= len(lines):
                raise Unsupported("[LIST] is never closed")
            i += 1
            rendered = "\n".join(f"<li>{inline(item, holes)}</li>" for item in items)
            blocks.append(f"<ul>\n{rendered}\n</ul>")
            continue

        heading = HEADING.fullmatch(line)
        if heading:
            level, body = heading.group(1), heading.group(2).strip()
            blocks.append(f"<h{level}>{inline(body, holes)}</h{level}>")
            i += 1
            continue

        # Paragraph: run to the next blank line or block tag. Soft line breaks inside a
        # paragraph are joined, matching how XenForo and Voxel both render them.
        para = [line]
        i += 1
        while i < len(lines):
            nxt = lines[i].strip()
            if not nxt or nxt.startswith(BLOCK_START):
                break
            para.append(nxt)
            i += 1
        blocks.append(f"<p>{inline(' '.join(para), holes)}</p>")

    out = "\n".join(blocks)
    for index, replacement in enumerate(holes):
        out = out.replace(f"\x00{index}\x00", replacement)

    unknown = sorted(set(LEFTOVER.findall(out)))
    if unknown:
        raise Unsupported(
            "unsupported BBCode tag(s): " + ", ".join(unknown)
            + " — add a rule to .github/scripts/bbcode-to-html.py"
        )

    # Numeric entities for everything non-ASCII (arrows, ellipses, accents). Voxel's own
    # editor does the same with named entities, and it makes the posted body immune to any
    # charset handling between here and the store page.
    return out.encode("ascii", "xmlcharrefreplace").decode("ascii")


def main(argv):
    if len(argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2
    with open(argv[1], encoding="utf-8") as handle:
        source = handle.read()
    try:
        print(convert(source))
    except Unsupported as error:
        print(f"{argv[1]}: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
