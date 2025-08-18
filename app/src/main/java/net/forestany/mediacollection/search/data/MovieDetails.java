package net.forestany.mediacollection.search.data;

public class MovieDetails {

    /* Fields */

    public boolean Adult;
    public String BackdropPath;
    public BelongsToCollection BelongsToCollection;
    public long Budget;
    public java.util.List<Genre> Genres = new java.util.ArrayList<Genre>();
    public String Homepage;
    public int Id;
    public String IMDBId;
    public String[] OriginCountry;
    public String OriginalLanguage;
    public String OriginalTitle;
    public String Overview;
    public double Popularity;
    public String PosterPath;
    public java.util.List<ProductionCompany> ProductionCompanies = new java.util.ArrayList<ProductionCompany>();
    public java.util.List<ProductionCountry> ProductionCountries = new java.util.ArrayList<ProductionCountry>();
    public String ReleaseDate;
    public long Revenue;
    public int Runtime;
    public java.util.List<SpokenLanguage> SpokenLanguages = new java.util.ArrayList<SpokenLanguage>();
    public String Status;
    public String Tagline;
    public String Title;
    public boolean Video;
    public double VoteAverage;
    public int VoteCount;
    public Credits Credits;

    /* Properties */

    /* Methods */

    public MovieDetails() {

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
