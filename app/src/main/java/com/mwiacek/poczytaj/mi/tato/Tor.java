package com.mwiacek.poczytaj.mi.tato;

public class Tor {
    Tor() {
/* https://www.apache.org/licenses/LICENSE-2.0
bindService(new Intent(this, TorService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                TorService torService = ((TorService.LocalBinder) service).getService();

                TorControlConnection conn = torService.getTorControlConnection();

                while ((conn = torService.getTorControlConnection()) == null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (conn != null) {
                    Toast.makeText(MainActivity.this, "Got Tor control connection", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(MainActivity.this, "Tor disconnected", Toast.LENGTH_LONG).show();

            }
        }, BIND_AUTO_CREATE);*/
    }
}
