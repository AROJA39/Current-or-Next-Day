package businesslogic;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * (En desuso) Clase para clasificar una transacción como 'CURRENT' o 'NEXT_DAY'.
 * NOTA: Clase en desuso. Por favor referirse a la nueva versión de esta clase: TransactionIdentificator2
 */
@Deprecated
@Data   // LINEA AGREGADA.
public class TransactionIdentificator {

    static String fechaYHoraCalendarioTransaccion = null;
    static String fechaCalendarioTransaccion = null;
    static String horaCalendarioTransaccion = null;
    static String fechaCompensacionTransaccion = null;
    static LocalDateTime fechaHoy = null;
    static Boolean esJornadaNormal = true;  // En día hábil es 'true'.
    public static Boolean esDiaHabil = true;    // AGREGAMOS 'PUBLIC'
    static final LocalTime FINAL_HOUR_FOR_NORMAL_JOURNEY = LocalTime.of(21,00,00);// Solamente aplica para día hábil (no sábados, ni domingos, ni festivos).
    static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMddHHmmss");
    static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
    static final Integer HORA_LIMITE_CIERRE = 0;
    static String estadoProceso = null;
    static String clasificacion;
    volatile static Boolean estaEnCierre = null;
    Map<String, Object> diasFestivos = new ConcurrentHashMap<>();
    static Map<String, Object> diasDePrueba = new HashMap<>();
    public static Map<String, DatosHashmap> datosTransaccionesPrueba = new HashMap<>();

    public void setNonWorkingDays(Map<String, Object> nonWorkingDaysMap){
        diasFestivos = new HashMap<>();
        if(nonWorkingDaysMap!=null && !nonWorkingDaysMap.isEmpty())
            diasFestivos.putAll(nonWorkingDaysMap);
    }

    public void inicializarDatos() {
        // Datos validos MMddHHmmss
        fechaYHoraCalendarioTransaccion = "0106110000";
        fechaCalendarioTransaccion = fechaYHoraCalendarioTransaccion.substring(0,4);
        horaCalendarioTransaccion = fechaYHoraCalendarioTransaccion.substring(4,10);
        // Datos válidos MMdd
        fechaCompensacionTransaccion = "0106";
        // Fecha del sistema
        fechaHoy = LocalDateTime.now();
        estaEnCierre = false;
        // Tipo de jornada hábil o no hábil
        esJornadaNormal = calcularSiEsJornadaNormal();
        // Indica si se trata de un día hábil
        esDiaHabil = true;
        // Estado del proceso
        estadoProceso = "";
        this.setNonWorkingDays(null);
        diasDePrueba.put("1230", "XFin de año BBOG (viernes)");
        diasDePrueba.put("0101", "Inicio de año (domingo)");
        diasDePrueba.put("1231", "Fin de año (sábado)");
        diasDePrueba.put("0102", "XPrimer día de 2023 (lunes)");
        diasDePrueba.put("0103", "XSegundo día de 2023 (martes)");
        diasDePrueba.put("0719", "XAntes del Día de la Independencia (miércoles)");
        diasDePrueba.put("0721", "XDespués del Día de la Independencia (viernes)");
        diasDePrueba.put("0806", "Antes del Día de la Batalla de Boyacá (domingo)");
        diasDePrueba.put("0808", "XDespués del Día de la Batalla de Boyacá (martes)");
    }

    public static final void armarDatosTransaccionesPrueba() {
        for( String llave : diasDePrueba.keySet() ) {
            boolean esHabil = ((String)diasDePrueba.get(llave)).startsWith("X");
            DatosHashmap datos = new DatosHashmap();
            datos.llave = llave;
            datos.strHoraNormal = getHoraAleatoria("JORNADA_NORMAL");
            datos.strFechaCalendario = llave + datos.strHoraNormal;
            datos.strFechaCompensacion = llave;
            datos.esCierre = false;
            datos.esDiaHabil = esHabil;
            datosTransaccionesPrueba.put(datos.strFechaCalendario, datos);
            datos = new DatosHashmap();
            datos.llave = llave;
            datos.strHoraCierre = getHoraAleatoria("JORNADA_CIERRE");
            datos.strFechaCalendarioCierre = llave + datos.strHoraCierre;
            datos.strFechaCompensacionCierre = llave;
            datos.esCierre = true;
            datos.esDiaHabil = esHabil;
            datosTransaccionesPrueba.put(datos.strFechaCalendarioCierre, datos);
        }
    }

    public static final String getHoraAleatoria( LocalTime initialTime, LocalTime finalTime ) {
        //LocalTime initialTime = LocalTime.of(8, 0, 0);
        //LocalTime finalTime = LocalTime.of(15, 59, 59);
        Random rand = new Random();
        int hours = rand.nextInt((finalTime.getHour() - initialTime.getHour()) + 1) + initialTime.getHour();
        int minutes =  rand.nextInt((finalTime.getMinute() - initialTime.getMinute()) + 1) + initialTime.getMinute();
        int seconds = rand.nextInt((finalTime.getSecond() - initialTime.getSecond()) + 1) + initialTime.getSecond();
        LocalTime random = LocalTime.of(hours,minutes,seconds);
        //System.out.println(random.toString());
        return random.toString().replaceAll("[:]", "");
   }

    public static String getHoraAleatoria( String rango ) {
        String aleatorioHora = "";
        switch( rango ) {
            case "JORNADA_NORMAL":
                aleatorioHora = getHoraAleatoria(LocalTime.of(3, 0, 0),LocalTime.of(20, 59, 59));
                break;
            case "JORNADA_CIERRE":
                aleatorioHora = getHoraAleatoria(LocalTime.of(21, 0, 0),LocalTime.of(23, 59, 59));
                break;
        }
        return aleatorioHora;
    }

    @Deprecated
    @Data
    public static class DatosHashmap {
        boolean esDiaHabil = true;
        boolean esCierre = false;
        String llave= "N/A";
        String strFechaCalendario= "N/A";
        String strFechaCompensacion= "N/A";
        String strFechaCalendarioCierre= "N/A";
        String strFechaCompensacionCierre= "N/A";
        String strHoraNormal= "N/A";
        String strHoraCierre= "N/A";

        @Override
        public String toString(){
            return "\tesDiaHabil:"+esDiaHabil
                    +"\tesCierre:"+esCierre
                    +"\tllave:"+llave
                    +"\tstrFechaCalendario:"+strFechaCalendario
                    +"\tstrFechaCompensacion:"+strFechaCompensacion
                    +"\tstrHoraNormal:"+strHoraNormal
                    +"\tstrFechaCalendarioCierre:"+strFechaCalendarioCierre
                    +"\tstrFechaCompensacionCierre:"+strFechaCompensacionCierre
                    +"\tstrHoraCierre:"+strHoraCierre;
        }
    }

    /* Método que recorra los días de prueba, y varíe la llaveNavegacion de compensación, la llaveNavegacion de la transacción,
    y si está en jornada normal encender o apagar el booleano esJornadaNormal. */

    private static Boolean calcularSiEsJornadaNormal(){
        if( estaEnCierre ){
            return false;
        }
        else{
            return true;
        }
    }

    private static String clasificarTransaccionComoCurrentONextDay(){
        if( fechaCompensacionTransaccion.equals( fechaCalendarioTransaccion ) ) {
            estadoProceso = "ES_TX_HOY";
        }
//        if( horaCalendarioTransaccion.compareTo( FINAL_HOUR_FOR_NORMAL_JOURNEY.format( timeFormatter ) ) <= HORA_LIMITE_CIERRE ){
//            estadoProceso = estadoProceso + "_SI_HORA_1";
//        }
        if( esJornadaNormal ){
            estadoProceso += "_ES_JORNADA_NORMAL";
        }
        if( esDiaHabil ){
            estadoProceso += "_ES_DIA_HABIL";
        }
        else{
            estadoProceso += "_ES_DIA_NO_HABIL";
        }

        if( estadoProceso.equals( "ES_TX_HOY_ES_JORNADA_NORMAL_ES_DIA_HABIL" )
                || estadoProceso.equals( "ES_TX_HOY_ES_JORNADA_NORMAL_ES_DIA_NO_HABIL" ) ){
            clasificacion = "CURRENT";
        }
        else{
            clasificacion = "NEXT_DAY";
        }
        return clasificacion;
    }

    /**
     * Método que clasifica una transacción como 'CURRENT' o 'NEXT_DAY'.
     * @param fechaCompensacionString Fecha de compensación de la transacción, en formato 'MMDD'.
     * @param fechaCalendarioString Fecha calendario, en formato 'MMDDHHmmss'
     * @return 'CURRENT' o 'NEXT_DAY'.
     */
    public static String clasificarTransaccionComoCurrentONextDay_v2(
            String fechaCompensacionString,
            String fechaCalendarioString
    ){
        if( !fechaCompensacionString.equals( fechaCalendarioString.substring(0,4) )
            || isClosedJourney()
        ){
            clasificacion = "NEXT_DAY";
        }
        else {
            clasificacion = "CURRENT";
        }
        return clasificacion;
    }

    /**
     * Función que valida las fechas recibidas como parámetros, y retorna el resultado del método DateDemo.clasificarTransaccionComoCurrentONextDay_v2()
     * @param fechaCompensacion Fecha de compensación en formato MMDD
     * @param fechaYHoraCalendario Fecha y hora calendario en formato MMDDHHmmss
     * @return Resultado del método DateDemo.clasificarTransaccionComoCurrentONextDay_v2()
     * @throws NullPointerException En el caso de que alguna de las fechas recibidas tenga valor nulo.
     * @throws DateTimeParseException En caso de que alguna de las fechas no tenga el formato indicado.
     */
    public String validateDatesAndclassifytransaction(String fechaCompensacion, String fechaYHoraCalendario) throws NullPointerException, DateTimeParseException {
        if(fechaCompensacion==null){
            throw new NullPointerException("La fecha de compensación no puede tener un valor nulo.");
        }
        if(fechaYHoraCalendario==null){
            throw new NullPointerException("La fecha calendario no puede tener un valor nulo.");
        }
        String regexMMDD = "((0[0-9])|(1[0-2]))((0[1-9])|(1[0-9])|(2[0-9])|(3[0-1]))";
        String regexMMDDHHmmSS = "((0[0-9])|(1[0-2]))((0[1-9])|(1[0-9])|(2[0-9])|(3[0-1]))(([0-1][0-9])|(2[0-3]))(([0-5][0-9]))(([0-5][0-9]))";
        Matcher matcherFechaCompensacion = Pattern.compile(regexMMDD).matcher(fechaCompensacion);
        Matcher matcherFechaYHoraCalendario = Pattern.compile(regexMMDDHHmmSS).matcher(fechaYHoraCalendario);
        if(!matcherFechaCompensacion.find()){
            throw new DateTimeParseException("La fecha de compensación tiene formato incorrecto. Formato correcto: 'MMDD'", "", 0);
        }
        if(!matcherFechaYHoraCalendario.find()){
            throw new DateTimeParseException("La fecha calendario tiene formato incorrecto. Formato correcto: 'MMDDHHmmss'", "", 0);
        }
        String classification = clasificarTransaccionComoCurrentONextDay_v2( fechaCompensacion, fechaYHoraCalendario );
        return classification ;
    }

    public synchronized static Boolean isClosedJourney(){
        LocalTime transactionHourReceived = LocalTime.now();
        if( isWorkingDay() ){
            if( transactionHourReceived.isAfter(FINAL_HOUR_FOR_NORMAL_JOURNEY)
            && !estaEnCierre){
                estaEnCierre = true;
            }
        }
        return estaEnCierre;
    }

    public static Boolean isWorkingDay(){
        //esDiaHabil = false;     // OJO. ESTA QUEMADO.
        return esDiaHabil;
    }

    /**
     * Configura el modo 'Cierre' como FALSE.
     */
    public static void apagarCierre(){
        estaEnCierre = false;
    }

    /**
     * Configua el modo 'Cierre' como TRUE.
     * Nota: usar solamente para pruebas, no en producción.
     */
    public static void activarCierreParaPruebas(){
        estaEnCierre = true;
    }

    /*public static void main(String args[]) {
        inicializarDatos();
        clasificarTransaccionComoCurrentONextDay();
        System.out.println("Clasificación: " + clasificacion);
        armarDatosTransaccionesPrueba();

        for( String llaveNavegacion : datosTransaccionesPrueba.keySet() ) {
            DatosHashmap datos = datosTransaccionesPrueba.get(llaveNavegacion);
            StringBuilder data = new StringBuilder();
            data //.append("[").append(llaveNavegacion ).append( "]->")
            .append("[").append( datos.llave ).append( "]->")
            .append("[").append( datos.strFechaCalendario ).append( "]")
            .append("[").append( datos.strFechaCompensacion ).append( "]")
            .append("[").append( datos.strHoraNormal ).append( "]")
            .append("[").append( datos.strFechaCalendarioCierre ).append( "]")
            .append("[").append( datos.strFechaCompensacionCierre ).append( "]")
            .append("[").append( datos.strHoraCierre ).append( "]");
            System.out.println("PRUEBA[" + data.toString() + "]");

            DateDemo.esDiaHabil = datos.esDiaHabil;
            if( datos.esCierre ) {
                DateDemo.fechaCompensacionTransaccion = datos.strFechaCompensacionCierre;
                DateDemo.fechaCalendarioTransaccion = datos.strFechaCalendarioCierre;
                DateDemo.esJornadaNormal = false;
            } else {
                DateDemo.fechaCompensacionTransaccion = datos.strFechaCompensacion;
                DateDemo.fechaCalendarioTransaccion = datos.strFechaCalendario;
                DateDemo.esJornadaNormal = true;
            }
            clasificarTransaccionComoCurrentONextDay();

            System.out.println("Clasificación: " + clasificacion);
        }
    }*/
}

