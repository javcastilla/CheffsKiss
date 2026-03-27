package software.ulpgc.cheffskiss.application.port.output

import java.util.UUID

interface CurrentUserPort {
    fun getCurrentUser(): UUID?
}


