Imports Microsoft.Data.SqlClient

Public Class LoginForm

    ' Keep encryption but accept the server cert (dev/testing)
    Dim connStr As String = "Server=localhost\SQLEXPRESS;Database=SampleDB;Trusted_Connection=True;TrustServerCertificate=True;"

    Private Sub btnLogin_Click(sender As Object, e As EventArgs) Handles btnLogin.Click

        Using conn As New SqlConnection(connStr)
            conn.Open()

            Dim query As String = "SELECT COUNT(*) FROM Users WHERE Username=@u AND Password=@p"

            Using cmd As New SqlCommand(query, conn)
                cmd.Parameters.AddWithValue("@u", txtUsername.Text)
                cmd.Parameters.AddWithValue("@p", txtPassword.Text)

                Dim count As Integer = Convert.ToInt32(cmd.ExecuteScalar())

                If count > 0 Then
                    MessageBox.Show("Login Successful")

                    Dim main As New MainForm
                    main.Show()
                    Me.Hide()
                Else
                    MessageBox.Show("Invalid Username or Password")
                End If
            End Using
        End Using

    End Sub

End Class
