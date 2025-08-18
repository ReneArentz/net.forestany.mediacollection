package net.forestany.mediacollection.search.data;

public class SeriesDetails {

    /* Fields */

    public boolean Adult;
    public String BackdropPath;
    public java.util.List<CreatedBy> CreatedBys = new java.util.ArrayList<CreatedBy>();
    public long[] EpisodeRunTime;
    public String FirstAirDate;
    public java.util.List<Genre> Genres = new java.util.ArrayList<Genre>();
    public String Homepage;
    public int Id;
    public boolean InProduction;
    public String[] Languages;
    public String LastAirDate;
    public LastEpisodeToAir LastEpisodeToAir;
    public String Name;
    public String NextEpisodeToAir;
    public java.util.List<Network> Networks = new java.util.ArrayList<Network>();
    public int NumberOfEpisodes;
    public int NumberOfSeasons;
    public String[] OriginCountry;
    public String OriginalLanguage;
    public String OriginalName;
    public String Overview;
    public double Popularity;
    public String PosterPath;
    public java.util.List<ProductionCompany> ProductionCompanies = new java.util.ArrayList<ProductionCompany>();
    public java.util.List<ProductionCountry> ProductionCountries = new java.util.ArrayList<ProductionCountry>();
    public java.util.List<Season> Seasons = new java.util.ArrayList<Season>();
    public java.util.List<SpokenLanguage> SpokenLanguages = new java.util.ArrayList<SpokenLanguage>();
    public String Status;
    public String Tagline;
    public String Type;
    public double VoteAverage;
    public int VoteCount;
    public Credits Credits;

    /* Properties */

    /* Methods */

    public SeriesDetails() {

    }

    @Override public String toString() {
        String s_foo = "";

        for (java.lang.reflect.Field o_field : this.getClass().getDeclaredFields()) {
            if (o_field.getName().startsWith("this$")) {
                continue;
            }

            try {
                if (o_field.get(this) instanceof java.util.List<?>) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> a_items = (java.util.List<Object>)o_field.get(this);

                    s_foo += o_field.getName() + " = [";

                    if (a_items.size() > 0) {
                        for (Object o_foo : a_items) {
                            s_foo += o_foo.toString() + ",";
                        }

                        s_foo = s_foo.substring(0, s_foo.length() - 1);
                    }

                    s_foo += "]|";
                } else if (o_field.getType().getTypeName().endsWith("[]")) {
                    String[] a_foo = new String[((String[])o_field.get(this)).length];
                    int i = 0;

                    for (String s_bar : (String[])o_field.get(this)) {
                        a_foo[i++] = s_bar;
                    }

                    s_foo += o_field.getName() + " = " + net.forestany.forestj.lib.Helper.printArrayList( java.util.Arrays.asList( (String[])a_foo ) ) + "|";
                } else {
                    s_foo += o_field.getName() + " = " + o_field.get(this).toString() + "|";
                }
            } catch (Exception o_exc) {
                s_foo += o_field.getName() + " = null|";
            }
        }

        s_foo = s_foo.substring(0, s_foo.length() - 1);

        return s_foo;
    }
}
