#/bin/bash

CURRENT=13000
END=16000
SKIP=5
TOTAL=4800

NAME=f
VERSION=1.0
DATE=`date -Iseconds`

POUT=patterns.json
AOUT=attributes.json
TOUT=test.json

HEADER="{\"TextGlassSpecVersion\": 1.0,\"type\":\"TYPE\",\"domain\":\"reference_$NAME\",\
\"domainVersion\":\"$VERSION\",\"description\":\"reference domain $NAME, performance\"\
,\"publishDate\":\"$DATE\","

echo $HEADER | sed "s/TYPE/pattern/" > $POUT
echo "\"inputParser\":{\"transformers\":[{\"type\":\"LowerCase\"}]," >> $POUT
echo "\"tokenSeperators\":[\" \",\",\",\";\"],\"ngramConcatSize\":2}," >> $POUT
echo "\"patternSet\":{\"defaultId\":\"unknown\",\"simpleHashCount\":$TOTAL,\"patterns\":[" >> $POUT

echo $HEADER | sed "s/TYPE/attribute/" > $AOUT
echo "\"attributes\":{" >> $AOUT

echo $HEADER | sed "s/TYPE/test/" > $TOUT
echo "\"tests\":[" >> $TOUT

while [ "$CURRENT" != "$END" ]
do
    ODDEVEN=odd
    ODDEVENP=o

    if [ "`expr $CURRENT % 2`" = "0" ]
    then
        ODDEVEN=even
        ODDEVENP=e
    fi

    LAST=,

    if [ "`expr $CURRENT + 1`" = "$END" ]
    then
        LAST=""
    fi

    if [ "`expr $CURRENT % $SKIP`" = "0" ]
    then

        #TEST
        echo "{\"input\":\"This number $CURRENT is unknown\",\"resultPatternId\":\"unknown\"}," >> $TOUT

        CURRENT=`expr $CURRENT + 1`
        continue
    fi

    #TEST
    echo "{\"input\":\"This number $CURRENT is unknown\",\"resultPatternId\":\"p$CURRENT\"," >> $TOUT
    echo "\"resultAttributes\":{\"number\":\"$CURRENT\",\"oe\":\"?\"}}," >> $TOUT
    echo "{\"input\":\"This number $CURRENT is $ODDEVEN\",\"resultPatternId\":\"p$CURRENT$ODDEVENP\"," >> $TOUT
    echo "\"resultAttributes\":{\"number\":\"oddeven\",\"oe\":\"$ODDEVEN\"}}$LAST" >> $TOUT

    #PATTERN
    echo "{\"patternId\":\"p$CURRENT\",\"rankType\":\"Weak\",\"patternType\":\"Simple\"," >> $POUT
    echo "\"patternTokens\":[\"$CURRENT\"]}," >> $POUT
    echo "{\"patternId\":\"p$CURRENT$ODDEVENP\",\"rankType\":\"Strong\"," >> $POUT
    echo "\"patternType\":\"SimpleOrderedAnd\",\"patternTokens\":[\"$CURRENT\",\"$ODDEVEN\"]}$LAST" >> $POUT

    #ATTRIBUTE
    echo "\"p$CURRENT\":{\"attributes\":{\"oe\":\"?\"}," >> $AOUT
    echo "\"attributeTransformers\":{\"number\":{\"transformers\":[{\"type\":\"SplitAndGet\"," >> $AOUT
    echo "\"parameters\":{\"delimeter\":\" \",\"get\":2}},{\"type\":\"IsNumber\"}]}}}," >> $AOUT
    echo "\"p$CURRENT$ODDEVENP\":{\"attributes\":{\"number\":\"oddeven\",\"oe\":\"$ODDEVEN\"}}$LAST" >> $AOUT

    CURRENT=`expr $CURRENT + 1`
done

echo "]}}" >> $POUT
echo "}}" >> $AOUT
echo "]}" >> $TOUT
