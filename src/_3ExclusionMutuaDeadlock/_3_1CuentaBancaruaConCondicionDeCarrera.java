package _3ExclusionMutuaDeadlock;

class CuentaBancaria {
    int saldo;

    public void depositar(int cantidad){
        this.saldo=saldo+cantidad;
    }
}
public class _3_1CuentaBancaruaConCondicionDeCarrera {
    public static void main(String[] args) {

        CuentaBancaria cb = new CuentaBancaria();
        for (int i = 1 ; i <= 100 ; i++ ){
            Thread hilo = new Thread(() -> {
                cb
            })
        }
    }
}
