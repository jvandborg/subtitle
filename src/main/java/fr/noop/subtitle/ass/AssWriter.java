package fr.noop.subtitle.ass;

import fr.noop.subtitle.ass.HexBGR.Color;
import fr.noop.subtitle.model.SubtitleCue;
import fr.noop.subtitle.model.SubtitleLine;
import fr.noop.subtitle.model.SubtitleObject;
import fr.noop.subtitle.model.SubtitleRegionCue;
import fr.noop.subtitle.model.SubtitleStyled;
import fr.noop.subtitle.model.SubtitleText;
import fr.noop.subtitle.model.SubtitleWriter;
import fr.noop.subtitle.util.SubtitleRegion;
import fr.noop.subtitle.util.SubtitleStyle;
import fr.noop.subtitle.util.SubtitleStyle.FontStyle;
import fr.noop.subtitle.util.SubtitleStyle.FontWeight;
import fr.noop.subtitle.util.SubtitleStyle.TextDecoration;
import fr.noop.subtitle.util.SubtitleTimeCode;
import fr.noop.subtitle.util.SubtitleRegion.VerticalAlign;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class AssWriter implements SubtitleWriter {
    private String charset; // Charset used to encode file

    public AssWriter(String charset) {
        this.charset = charset;
    }

    @Override
    public void write(SubtitleObject subtitleObject, OutputStream os, String outputTimecode) throws IOException {
        try {
            // Write Script Info
            this.writeScriptInfo(subtitleObject, os);

            // Write Style
            this.writeV4Styles(os);

            // Write cues
            this.writeEvents(subtitleObject, os, outputTimecode);
        } catch (UnsupportedEncodingException e) {
            throw new IOException("Encoding error in input subtitle");
        }
    }

    private void writeScriptInfo(SubtitleObject subtitleObject, OutputStream os) throws IOException {
        os.write(new String("[Script Info]\n").getBytes(this.charset));
        os.write(new String("; Script generated by Nomalab Subtitle library\n").getBytes(this.charset));
        if (subtitleObject.hasProperty(SubtitleObject.Property.TITLE)) {
            // Write title
            os.write(String.format("Title: %s\n",
                    subtitleObject.getProperty(SubtitleObject.Property.TITLE)
            ).getBytes(this.charset));
        }
        os.write(new String("ScriptType: v4.00+\n").getBytes(this.charset));
        os.write(new String("PlayDepth: 0\n").getBytes(this.charset));
        os.write(new String("PlayResX: 1920\n").getBytes(this.charset));
        os.write(new String("PlayResY: 1080\n").getBytes(this.charset));
        os.write(new String("\n").getBytes(this.charset));
    }

    private void writeV4Styles(OutputStream os) throws IOException {
        os.write(new String("[V4+ Styles]\n").getBytes(this.charset));
        os.write(new String(
                "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n"
        ).getBytes(this.charset));
        os.write(new String(
                "Style: Nomalab_Default,Arial,52,&H00FFFFFF,&H0300FFFF,&H00000000,&H02000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1\n"
        ).getBytes(this.charset));
        os.write(new String("\n").getBytes(this.charset));
    }

    private void writeEvents(SubtitleObject subtitleObject, OutputStream os, String outputTimecode) throws IOException {
        SubtitleTimeCode startTimecode = new SubtitleTimeCode(0);
        if (subtitleObject.hasProperty(SubtitleObject.Property.START_TIMECODE_PRE_ROLL)) {
            startTimecode = (SubtitleTimeCode) subtitleObject.getProperty(SubtitleObject.Property.START_TIMECODE_PRE_ROLL);
        }
        float frameRate = 25;
        if (subtitleObject.hasProperty(SubtitleObject.Property.FRAME_RATE)) {
            frameRate = (float) subtitleObject.getProperty(SubtitleObject.Property.FRAME_RATE);
        }
        os.write(new String("[Events]\n").getBytes(this.charset));
        os.write(new String(
                "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n"
        ).getBytes(this.charset));
        for (SubtitleCue cue : subtitleObject.getCues()) {
            String cueText = "";

            SubtitleTimeCode startTC = cue.getStartTime();
            if (outputTimecode != null) {
                SubtitleTimeCode outputTC = SubtitleTimeCode.fromStringWithFrames(outputTimecode, frameRate);
                startTC = cue.getStartTime().convertFromStart(outputTC, startTimecode);
            }
            SubtitleTimeCode endTC = cue.getEndTime();
            if (outputTimecode != null) {
                SubtitleTimeCode outputTC = SubtitleTimeCode.fromStringWithFrames(outputTimecode, frameRate);
                endTC = cue.getEndTime().convertFromStart(outputTC, startTimecode);
            }

            int vp = ((SubtitleRegionCue) cue).getRegion().getVerticalPosition();
            cueText += addStyle(cue);

            int lineIndex = 0;
            for (SubtitleLine line : cue.getLines()) {
                lineIndex++;
                for (SubtitleText text : line.getTexts()) {
                    cueText += text.toString();
                    // Add line break between rows
                    if (lineIndex < cue.getLines().size()) {
                        cueText += "\\N";
                    }
                }
            }

            os.write(String.format("Dialogue: 0,%s,%s,Nomalab_Default,,0,0,%d,,%s\n",
                    startTC.singleHourTimeToString(), endTC.singleHourTimeToString(), vp, cueText
            ).getBytes(this.charset));
        }
        os.write(new String("\n").getBytes(this.charset));
    }

    private String addStyle(SubtitleCue cue) {
        SubtitleRegion region = ((SubtitleRegionCue) cue).getRegion();
        String styled = "{";
        int posX = 1920 / 2;
        // FIXME : use Math.round(1080 * region.getHeight() / 100) and recalculate height region in StlObject
        int posY = 1080 - Math.round(region.getHeight());
        if (region.getVerticalAlign() == VerticalAlign.TOP) {
            int lines = cue.getLines().size();
            posY = Math.round(1080 * region.getHeight() / 100) + 52 * lines;
        }
        String position = String.format("\\pos(%d,%d)", posX, posY);
        SubtitleText firstLineText = cue.getLines().get(0).getTexts().get(0);

        styled += position;
        if (firstLineText instanceof SubtitleStyled) {
            SubtitleStyle style = ((SubtitleStyled) firstLineText).getStyle();
            if (style.getFontStyle() == FontStyle.ITALIC || style.getFontStyle() == FontStyle.OBLIQUE) {
                styled += "\\i1";
            }
            if (style.getFontWeight() == FontWeight.BOLD) {
                styled += "\\b1";
            }
            if (style.getTextDecoration() == TextDecoration.UNDERLINE) {
                styled += "\\u1";
            }
            if (style.getColor() != null){
                Color color = HexBGR.Color.getEnumFromName(style.getColor());
                styled += String.format("\\c%s", color.getHexValue());
            }
        }
        styled += "}";
        return styled;
    }
}
