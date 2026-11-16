package ar.edu.unc.fcefyn.pcp.tpfinal.politica;
import java.util.concurrent.ThreadLocalRandom; //Libreria para generar numeros aleatorios

public class RandomPolicy implements Politica {
    @Override
    public boolean[] permitidas(boolean[] sensibilizadas) {
        return sensibilizadas.clone();
    }
    @Override
    public int elegir(boolean[] candidatas){
        int cantidad=0;
        for(int i =0;i<candidatas.length;i++){
            if(candidatas[i]){
                cantidad++;
            }
        }
        if(cantidad==0){
            return -1;
        }
        int posicion = ThreadLocalRandom.current().nextInt(cantidad); //Si hay dos candidatas, posicion vale 0 o 1
        for(int i =0;i<candidatas.length;i++){
            if(candidatas[i]){
                if(posicion==0){
                    return i;
                }
                posicion--;
            }
        }
        throw new IllegalStateException("No se encontró la candidata sorteada");
    }

}
