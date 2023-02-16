package businesslogic;


import lombok.Data;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase para identificación de transacciones como 'CURRENT' o 'NEXT_DAY'.
 */
@Data
public class TransactionIdentificator2 {
    private volatile Boolean isWorkingDay = true;   // Variable para indicar si el sistema está en día laborable.
    private final LocalTime FINAL_HOUR_FOR_NORMAL_JOURNEY = LocalTime.of(21,00,00);// Solamente aplica para día hábil (no sábados, ni domingos, ni festivos).
    private volatile Boolean isInClosingJourney = false;   // Variable para indicar si el sistema está en modo 'En Cierre'.
    Map<String, Object> nonWorkingDays = new ConcurrentHashMap<>(); // Variable para almacenar una lista de días no laborales (festivos).

    /**
     * Método que recibe una lista de pares llave-valor (de clase Map) con días no laborales.
     * En dicha lista, cada llave corresponde a un mes y día del mes en formato MMDD, y cada valor corresponde a un texto descriptivo del mes y día correspondiente.
     * @param nonWorkingDaysMap Lista de pares llave-valor donde las llaves son los días no laborales, y los valores son los textos descriptivos de cada día no laborable.
     * @throws NullPointerException Si la lista de pares llave-valor de días no laborales tiene valor nulo.
     * @throws UnsupportedOperationException Si la lista de pares llave-valor de días no laborales está vacía.
     */
    public synchronized void setNonWorkingDays(Map<String, Object> nonWorkingDaysMap) throws NullPointerException, UnsupportedOperationException{
        if(nonWorkingDays==null){
            throw new NullPointerException("La lista de días no laborales no puede tener un valor nulo.");
        }
        if(nonWorkingDays.isEmpty()){
            throw new UnsupportedOperationException("La lista de días no laborales no puede ser una lista vacía.");
        }
        nonWorkingDays.putAll(nonWorkingDaysMap);
    }

    /**
     * Función que valida las fechas recibidas como parámetros, y clasifica la transacción como 'CURRENT' o 'NEXT_DAY'.
     * @param fechaCompensacion Fecha de compensación en formato MMDD
     * @param fechaYHoraCalendario Fecha y hora calendario en formato MMDDHHmmss
     * @return Resultado del método DateDemo.clasificarTransaccionComoCurrentONextDay_v2()
     * @throws NullPointerException En el caso de que alguna de las fechas recibidas tenga valor nulo.
     * @throws DateTimeParseException En caso de que alguna de las fechas no tenga el formato indicado.
     */
    public String validateDatesAndclassifytransaction(String fechaCompensacion, String fechaYHoraCalendario) throws NullPointerException, DateTimeParseException {
        if(fechaCompensacion==null){
            throw new NullPointerException("Mensaje desde " + this.getClass().getSimpleName()
                    + "La fecha de compensación no puede tener un valor nulo."
            );
        }
        if(fechaYHoraCalendario==null){
            throw new NullPointerException("Mensaje desde " + this.getClass().getSimpleName()
                    + ": La fecha y hora calendario no puede tener un valor nulo."
            );
        }
        String regexMMDD = "((0[0-9])|(1[0-2]))((0[1-9])|(1[0-9])|(2[0-9])|(3[0-1]))";
        String regexMMDDHHmmSS = "((0[0-9])|(1[0-2]))((0[1-9])|(1[0-9])|(2[0-9])|(3[0-1]))(([0-1][0-9])|(2[0-3]))(([0-5][0-9]))(([0-5][0-9]))";
        Matcher matcherFechaCompensacion = Pattern.compile(regexMMDD).matcher(fechaCompensacion);
        Matcher matcherFechaYHoraCalendario = Pattern.compile(regexMMDDHHmmSS).matcher(fechaYHoraCalendario);
        if(!matcherFechaCompensacion.find()){
            throw new DateTimeParseException("Mensaje desde " + this.getClass().getSimpleName() + ": "
                    + "La fecha de compensación tiene formato incorrecto. Formato correcto: 'MMDD'", "", 0);
        }
        if(!matcherFechaYHoraCalendario.find()){
            throw new DateTimeParseException("Mensaje desde " + this.getClass().getSimpleName() + ": "
                    + "La fecha calendario tiene formato incorrecto. Formato correcto: 'MMDDHHmmss'", "", 0);
        }
        String classification;
        if( !fechaYHoraCalendario.startsWith(fechaCompensacion) || isClosedJourney() ){
            classification = "NEXT_DAY";
        }
        else {
            classification = "CURRENT";
        }
        return classification ;
    }

    /**
     * Método que retorna si el microservicio está en modo 'Cierre' o no.
     * @return TRUE si está en modo 'Cierre'. FALSE en caso contrario.
     */
    public synchronized Boolean isClosedJourney(){
        LocalTime transactionHourReceived = LocalTime.now();
        if( isWorkingDay() && transactionHourReceived.isAfter(FINAL_HOUR_FOR_NORMAL_JOURNEY) && !isInClosingJourney){
            isInClosingJourney = true;
        }
        return isInClosingJourney;
    }

    /**
     * Método que retorna si el microservicio está en día laborable.
     * @return TRUE si el microservicio está en día laborable; FALSE en caso contrario.
     */
    public synchronized Boolean isWorkingDay(){
        return isWorkingDay;
    }

    /**
     * Método para configurar el microservicio en modo 'Cierre' como FALSE (modo 'Cierre' desactivado).
     * @return FALSE el cual es el valor asignado para el modo 'Cierre'.
     */
    public synchronized Boolean apagarEnCierre(){
        isInClosingJourney = false;
        return isInClosingJourney;
    }

    /**
     * Método para configurar el microservicio en modo 'Cierre' como TRUE (modo 'Cierre' activado).
     * @return TRUE el cual es el valor asignado para el modo 'Cierre'.
     */
    public synchronized Boolean activarEnCierre(){
        isInClosingJourney = true;
        return isInClosingJourney;
    }
}
