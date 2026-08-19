#!/usr/bin/env python3
"""Convert a MyPet changelog from XenForo BBCode to the Markdown Discord renders.

The third rendering of one source file. BuiltByBit (XenForo) takes the changelog as
BBCode, Voxel.Shop stores it as HTML (see bbcode-to-html.py), and the MyPet Hub puts it
in a Discord embed description, which is Markdown. Before this script the Hub got no
changelog at all and its release announcement listed raw commit subjects instead.

Only a slice of the changelog is sent, via --from-heading: a Discord embed description is
capped at 4096 characters and a full changelog is most of that on its own, so the prose
sections stay on the store pages the announcement already links, and Discord gets the
"Full changelog:" bullets — which is what that embed was already rendering, in commit
subjects, before it had player-facing text to use instead.

Deliberately strict, for the same reason as its HTML sibling: an unrecognised BBCode tag,
a missing --from-heading, or an over-budget result is an error, not something to pass
through, drop or truncate. The release workflow runs this before it tags, uploads or posts
anything, so any of those fails the run while it is still free to re-dispatch.

Markdown metacharacters in the prose are deliberately NOT escaped: the [B] and [I] tags
exist to produce exactly those characters, and MyPet's changelogs write anything that
would collide (config paths, permission nodes) inside [ICODE], which becomes a code span.

Usage: bbcode-to-discord.py <changelog.bbcode> [--from-heading TEXT] [--max-chars N]
"""
import re
import sys

HEADING = re.compile(r"\[HEADING=([1-6])\](.*?)\[/HEADING\]", re.DOTALL)
URL_TAG = re.compile(r"\[URL=([^\]]+)\](.*?)\[/URL\]", re.DOTALL)
ICODE = re.compile(r"\[ICODE\](.*?)\[/ICODE\]", re.DOTALL)
BOLD = re.compile(r"\[B\](.*?)\[/B\]", re.DOTALL)
ITALIC = re.compile(r"\[I\](.*?)\[/I\]", re.DOTALL)
# Anything that still looks like a tag after conversion is a tag we don't handle.
LEFTOVER = re.compile(r"\[/?[A-Za-z*][^\]]*\]")
BLOCK_START = ("[LIST", "[HEADING")
# Discord renders # through ### only; deeper levels fall back to the last real heading.
MAX_HEADING_LEVEL = 3
# Discord's embed description limit. BuildMessageComposer truncates at 4000 with an
# ellipsis; failing here instead means an over-budget changelog is caught before the
# release publishes anywhere, rather than reaching players cut off mid-sentence.
DEFAULT_MAX_CHARS = 4000


class Unsupported(Exception):
    pass


def inline(text, holes):
    """Convert the inline tags in one already-joined block of text."""

    def stash(replacement):
        """Park finished Markdown so later passes can't reprocess its contents."""
        holes.append(replacement)
        return f"\x00{len(holes) - 1}\x00"

    def render_url(match):
        # XenForo writes both [URL=x] and [URL='x']; the quotes are delimiters, not href.
        href = match.group(1).strip().strip("\"'")
        return stash(f"[{inline(match.group(2), holes)}]({href})")

    out = URL_TAG.sub(render_url, text)
    # Code spans are stashed before the bold/italic passes so a * or _ inside one stays
    # literal, which is the whole point of writing it as [ICODE] in the first place.
    out = ICODE.sub(lambda m: stash(f"`{m.group(1)}`"), out)
    out = BOLD.sub(lambda m: f"**{m.group(1)}**", out)
    out = ITALIC.sub(lambda m: f"*{m.group(1)}*", out)
    # Bare URLs are left alone: Discord auto-links them, and a masked link would need a
    # label this script has no way to invent.
    return out


def slice_from_heading(lines, wanted):
    """Return the lines after the [HEADING] whose text matches `wanted`.

    The heading itself is dropped — the embed already carries a title, so repeating
    "Full changelog:" as the first line of the description just wastes budget.
    """
    target = wanted.strip().casefold()
    for index, line in enumerate(lines):
        heading = HEADING.fullmatch(line.strip())
        if heading and heading.group(2).strip().casefold() == target:
            return lines[index + 1:]
    raise Unsupported(f"no [HEADING] matching {wanted!r} — nothing to send")


def convert(source, from_heading=None, max_chars=DEFAULT_MAX_CHARS):
    if "\x00" in source:
        raise Unsupported("changelog contains a NUL byte")

    holes = []
    lines = source.split("\n")
    if from_heading is not None:
        lines = slice_from_heading(lines, from_heading)
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
            blocks.append("\n".join(f"- {inline(item, holes)}" for item in items))
            continue

        heading = HEADING.fullmatch(line)
        if heading:
            level = min(int(heading.group(1)), MAX_HEADING_LEVEL)
            blocks.append(f"{'#' * level} {inline(heading.group(2).strip(), holes)}")
            i += 1
            continue

        # Paragraph: run to the next blank line or block tag. Soft line breaks inside a
        # paragraph are joined, matching how the HTML sibling and XenForo render them.
        para = [line]
        i += 1
        while i < len(lines):
            nxt = lines[i].strip()
            if not nxt or nxt.startswith(BLOCK_START):
                break
            para.append(nxt)
            i += 1
        blocks.append(inline(" ".join(para), holes))

    out = "\n\n".join(blocks)
    for index, replacement in enumerate(holes):
        out = out.replace(f"\x00{index}\x00", replacement)

    unknown = sorted(set(LEFTOVER.findall(out)))
    if unknown:
        raise Unsupported(
            "unsupported BBCode tag(s): " + ", ".join(unknown)
            + " — add a rule to .github/scripts/bbcode-to-discord.py"
        )

    if len(out) > max_chars:
        raise Unsupported(
            f"converts to {len(out)} characters, over the {max_chars} Discord allows —"
            " shorten the changelog section or split it across releases"
        )
    return out


def main(argv):
    args = argv[1:]
    from_heading = None
    max_chars = DEFAULT_MAX_CHARS
    path = None
    while args:
        arg = args.pop(0)
        if arg == "--from-heading":
            if not args:
                print("--from-heading needs a value", file=sys.stderr)
                return 2
            from_heading = args.pop(0)
        elif arg == "--max-chars":
            if not args:
                print("--max-chars needs a value", file=sys.stderr)
                return 2
            max_chars = int(args.pop(0))
        elif path is None:
            path = arg
        else:
            print(__doc__, file=sys.stderr)
            return 2
    if path is None:
        print(__doc__, file=sys.stderr)
        return 2

    with open(path, encoding="utf-8") as handle:
        source = handle.read()
    try:
        print(convert(source, from_heading, max_chars))
    except Unsupported as error:
        print(f"{path}: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
