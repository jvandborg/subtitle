/*
 *  This file is part of the noOp organization .
 *
 *  (c) Cyrille Lebeaupin <clebeaupin@noop.fr>
 *
 *  For the full copyright and license information, please view the LICENSE
 *  file that was distributed with this source code.
 *
 */

package fr.noop.subtitle.vtt;

import fr.noop.subtitle.model.SubtitleCue;
import fr.noop.subtitle.model.SubtitleLine;
import fr.noop.subtitle.model.SubtitleObject;
import fr.noop.subtitle.model.SubtitleRegionCue;
import fr.noop.subtitle.model.SubtitleStyled;
import fr.noop.subtitle.model.SubtitleText;
import fr.noop.subtitle.model.SubtitleWriterWithTimecode;
import fr.noop.subtitle.util.SubtitleStyle;
import fr.noop.subtitle.util.SubtitleTimeCode;
import fr.noop.subtitle.util.SubtitleRegion.VerticalAlign;
import fr.noop.subtitle.util.SubtitleStyle.FontStyle;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by clebeaupin on 11/10/15.
 */
public class VttWriter implements SubtitleWriterWithTimecode {
    private String charset; // Charset used to encode file
    private String outputTimecode;

    public VttWriter(String charset) {
        this.charset = charset;
    }

    @Override
    public void write(SubtitleObject subtitleObject, OutputStream os) throws IOException {
        try {
            // Write header
            os.write(new String("WEBVTT\n\n").getBytes(this.charset));

            // Write cues
            for (SubtitleCue cue : subtitleObject.getCues()) {
                if (cue.getId() != null) {
                    // Write number of subtitle
                    String number = String.format("%s\n", cue.getId());
                    os.write(number.getBytes(this.charset));
                }
                
                // Write Start time and end time
                String startToEnd = null;
                SubtitleTimeCode startTC = cue.getStartTime();
                SubtitleTimeCode endTC = cue.getEndTime();
                SubtitleTimeCode startTimeCode = new SubtitleTimeCode(0);
                float frameRate = 25;
                if (subtitleObject.hasProperty(SubtitleObject.Property.START_TIMECODE_PRE_ROLL)){
                    startTimeCode = (SubtitleTimeCode) subtitleObject.getProperty(SubtitleObject.Property.START_TIMECODE_PRE_ROLL);
                }
                if (subtitleObject.hasProperty(SubtitleObject.Property.FRAME_RATE)) {
                    frameRate = (float) subtitleObject.getProperty(SubtitleObject.Property.FRAME_RATE);
                }
                if (outputTimecode != null) {
                    SubtitleTimeCode outputTC = SubtitleTimeCode.fromStringWithFrames(outputTimecode, frameRate);
                    startTC = cue.getStartTime().convertFromStart(outputTC, startTimeCode);
                    endTC = cue.getEndTime().convertFromStart(outputTC, startTimeCode);
                }
                startToEnd = String.format("%s --> %s%s\n",
                    this.formatTimeCode(startTC),
                    this.formatTimeCode(endTC),
                    this.verticalPosition(cue));

                os.write(startToEnd.getBytes(this.charset));
                // Write text
                //String text = String.format("%s\n", cue.getText());

                String text = "";
                for (SubtitleLine line : cue.getLines()) {
                    for (SubtitleText inText : line.getTexts()) {
                        if (inText instanceof SubtitleStyled) {
                            SubtitleStyle style = ((SubtitleStyled)inText).getStyle();
                            String textString = inText.toString();
                            if (style.getFontStyle() == FontStyle.ITALIC || style.getFontStyle() == FontStyle.OBLIQUE) {
                                textString = String.format("<i>%s</i>", textString);
                            }
                            if (style.getColor() != null){
                                textString = String.format("<c.%s>%s</c>", style.getColor(), textString);
                            }
                            text += textString;
                        } else {
                            text += inText.toString();
                        }
                        text += "\n";
                    }
                }
                os.write(text.getBytes(this.charset));

                // Write empty line
                os.write("\n".getBytes(this.charset));

                // Get region

            }
        } catch (UnsupportedEncodingException e) {
            throw new IOException("Encoding error in input subtitle");
        }
    }

    private String verticalPosition(SubtitleCue cue) {
        if (cue instanceof SubtitleRegionCue) {
            VerticalAlign va =  ((SubtitleRegionCue) cue).getRegion().getVerticalAlign();
            if (va == VerticalAlign.TOP) {
                return " line:0";
            }
            else {
                return "";
            }
        }
        return "";
    }
    private String formatTimeCode(SubtitleTimeCode timeCode) {
        return String.format("%02d:%02d:%02d.%03d",
                timeCode.getHour(),
                timeCode.getMinute(),
                timeCode.getSecond(),
                timeCode.getMillisecond());
    }

    @Override
    public void setTimecode(String timecode) {
        this.outputTimecode= timecode;
    }
}
