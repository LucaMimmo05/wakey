package it.luca.sveglia.weather;

/**
 * Traduce i codici meteo WMO usati da Open-Meteo in frasi italiane.
 * Tabella dei codici: https://open-meteo.com/en/docs (sezione "WMO Weather interpretation codes").
 */
public final class WeatherDescriptions {

    private WeatherDescriptions() {
    }

    public static String describe(int code) {
        switch (code) {
            case 0:
                return "cielo sereno";
            case 1:
                return "cielo poco nuvoloso";
            case 2:
                return "cielo parzialmente nuvoloso";
            case 3:
                return "cielo coperto";
            case 45:
            case 48:
                return "nebbia";
            case 51:
            case 53:
            case 55:
                return "pioggerella";
            case 56:
            case 57:
                return "pioggerella gelata";
            case 61:
                return "pioggia debole";
            case 63:
                return "pioggia";
            case 65:
                return "pioggia forte";
            case 66:
            case 67:
                return "pioggia gelata";
            case 71:
                return "neve debole";
            case 73:
                return "neve";
            case 75:
                return "neve forte";
            case 77:
                return "nevischio";
            case 80:
            case 81:
                return "rovesci di pioggia";
            case 82:
                return "rovesci di pioggia violenti";
            case 85:
            case 86:
                return "rovesci di neve";
            case 95:
                return "temporale";
            case 96:
            case 99:
                return "temporale con grandine";
            default:
                return "tempo variabile";
        }
    }
}
