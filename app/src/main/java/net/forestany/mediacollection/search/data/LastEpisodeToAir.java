package net.forestany.mediacollection.search.data;

public class LastEpisodeToAir {

    /* Fields */

    public int Id;
    public String Name;
    public String Overview;
    public double VoteAverage;
    public int VoteCount;
    public String AirDate;
    public int EpisodeNumber;
    public String EpisodeType;
    public String ProductionCode;
    public int Runtime;
    public int SeasonNumber;
    public int ShowId;
    public String StillPath;

    /* Properties */

    /* Methods */

    public LastEpisodeToAir() {

    }

    @Override public String toString() {
        String s_foo = "";

        for (java.lang.reflect.Field o_field : this.getClass().getDeclaredFields()) {
            if (o_field.getName().startsWith("this$")) {
                continue;
            }

            try {
                s_foo += o_field.getName() + " = " + o_field.get(this).toString() + "|";
            } catch (Exception o_exc) {
                s_foo += o_field.getName() + " = null|";
            }
        }

        s_foo = s_foo.substring(0, s_foo.length() - 1);

        return s_foo;
    }
}
